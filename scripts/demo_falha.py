"""Verificação destrutiva apenas dos processos da pilha de demonstração indicada."""
import argparse
import subprocess
import threading
import time
import uuid
from concurrent.futures import ThreadPoolExecutor

from chat_cliente import Cliente, esperar, historico


def registrar(texto):
    print(time.strftime("%H:%M:%S") + " " + texto, flush=True)


def compose(*argumentos):
    return subprocess.check_output(["docker", "compose", *argumentos], text=True).strip()


def sql(comando):
    return compose("exec", "-T", "postgres", "psql", "-v", "ON_ERROR_STOP=1", "-At",
                   "-U", "receitopedia", "-d", "receitopedia", "-c", comando)


def fila(nome):
    linhas = compose("exec", "-T", "rabbitmq", "rabbitmqctl", "-q", "list_queues",
                     "--no-table-headers", "name", "messages_ready", "messages_unacknowledged").splitlines()
    for linha in linhas:
        partes = linha.split()
        if partes[0:1] == [nome]:
            return int(partes[1]), int(partes[2])
    return 0, 0


def historico_completo(cliente, conversa):
    rota = historico(conversa)
    pagina = cliente.chamar("GET", rota)
    itens = list(pagina)
    while len(pagina) == 30:
        pagina = cliente.chamar("GET", rota + "?antes=" + str(pagina[-1]["sequencia"]))
        itens.extend(pagina)
    return itens


def checar_entrega(destino, conversa, ids):
    esperados = set(ids)

    def comparar():
        mensagens = historico_completo(destino, conversa)
        presentes = [m["id"] for m in mensagens if m["id"] in esperados]
        return len(presentes) == len(esperados) and set(presentes) == esperados

    esperar(comparar, 100)
    persistidas = int(sql(f"select count(*) from chat_mensagem where conversa_id = '{conversa}'"))
    assert persistidas == len(esperados), (persistidas, len(esperados))
    registrar(f"Enviadas={len(esperados)} persistidas={persistidas} entregues={len(esperados)} duplicadas=0")


def enviar_retentando(cliente, conversa, id_msg, texto):
    for tentativa in range(20):
        try:
            return cliente.enviar(conversa, texto, id_msg)
        except (OSError, AssertionError):
            if tentativa == 19:
                raise
            time.sleep(min(.25 * (tentativa + 1), 2))


def ciclo(base, servico):
    a = Cliente(base).criar_conta()
    b = Cliente(base).criar_conta()
    conversa = a.dm(b)
    recebeu_um = threading.Event()
    parada_executada = threading.Event()
    continuar = threading.Event()
    ids = []

    def enviar():
        while not continuar.is_set():
            id_msg = str(uuid.uuid4())
            enviar_retentando(a, conversa, id_msg, f"Falha {servico}, envio {len(ids) + 1}")
            ids.append(id_msg)
            recebeu_um.set()
            if parada_executada.is_set() and len(ids) >= 9:
                return
            time.sleep(.20)

    with ThreadPoolExecutor(max_workers=1) as executor:
        futuro = executor.submit(enviar)
        try:
            assert recebeu_um.wait(30)
            registrar(f"Interrompendo {servico} durante os envios")
            compose("stop", "-t", "0", servico)
            parada_executada.set()
            futuro.result(timeout=95)
            if servico == "worker":
                prontas = esperar(lambda: fila("receitopedia.eventos.trabalho")[0])
                registrar(f"Eventos na fila com worker parado: {prontas}")
            if servico == "rabbitmq":
                pendentes = int(sql(f"""select count(*) from evento_outbox
                    where agregado = '{conversa}' and publicado_em is null"""))
                assert pendentes > 0
                registrar(f"Eventos no outbox com broker parado: {pendentes}")
        finally:
            continuar.set()
            compose("start", servico)
            registrar(f"{servico} reiniciado")
    checar_entrega(b, conversa, ids)


def testar_dlq(base):
    a = Cliente(base).criar_conta()
    b = Cliente(base).criar_conta()
    conversa = a.dm(b)
    id_msg = str(uuid.uuid4())
    gatilho = "demo_fccpd_" + uuid.uuid4().hex[:12]
    antes = fila("receitopedia.eventos.dlq")[0]
    assert antes == 0, "Use uma pilha de teste com DLQ vazia."
    sql(f"""
        create function {gatilho}() returns trigger language plpgsql as $$
        begin
            if new.id = '{id_msg}' and new.processada then
                raise exception 'Falha temporaria de processamento para a demonstracao FCCPD';
            end if;
            return new;
        end;
        $$;
        create trigger {gatilho} before update on chat_mensagem for each row execute function {gatilho}();
        """)
    try:
        a.enviar(conversa, "Mensagem para demonstrar retry e DLQ", id_msg)
        esperar(lambda: fila("receitopedia.eventos.dlq")[0] == antes + 1, 70)
        assert not b.chamar("GET", historico(conversa))
        registrar("Um evento foi para a DLQ após o processamento falhar nas três tentativas.")
    finally:
        sql(f"drop trigger {gatilho} on chat_mensagem; drop function {gatilho}();")

    resultado = compose("run", "--rm", "--no-deps", "-e", "RECEITOPEDIA_EVENTOS_WORKER=false",
                        "-e", "RECEITOPEDIA_EVENTOS_REPROCESSAR=true", "worker")
    registrar(next(linha for linha in resultado.splitlines() if "Eventos reenviados" in linha))
    checar_entrega(b, conversa, [id_msg])
    assert fila("receitopedia.eventos.dlq")[0] == antes

    # Republica o mesmo evento durável para simular a janela entre commit e ack.
    sql(f"""update evento_outbox set publicado_em = null
        where tipo = 'mensagem.enviada' and referencia = '{id_msg}'""")
    esperar(lambda: sql(f"""select publicado_em is not null from evento_outbox
        where tipo = 'mensagem.enviada' and referencia = '{id_msg}'""") == "t")
    esperar(lambda: fila("receitopedia.eventos.trabalho") == (0, 0))
    assert sql(f"""select count(*) from evento_processado p join evento_outbox e on e.id = p.id
        where e.referencia = '{id_msg}'""") == "1"
    assert sql(f"select count(*) from chat_recibo where mensagem_id = '{id_msg}'") == "1"
    checar_entrega(b, conversa, [id_msg])
    registrar("Redelivery confirmado sem duplicar a mensagem e o recibo.")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", default="http://localhost:8080")
    parser.add_argument("--sem-rabbit", action="store_true")
    parser.add_argument("--somente-dlq", action="store_true")
    args = parser.parse_args()
    if not args.somente_dlq:
        for servico in ["worker", "receitopedia"] + ([] if args.sem_rabbit else ["rabbitmq"]):
            ciclo(args.base, servico)
    testar_dlq(args.base)


if __name__ == "__main__":
    main()
