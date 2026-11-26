"""Cliente HTTP para os roteiros de verificação das mensagens."""
import http.cookiejar
import json
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid


class Cliente:
    def __init__(self, base, cookies=None):
        self.base = base.rstrip("/")
        self.cookies = cookies if cookies is not None else http.cookiejar.CookieJar()
        self.http = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies))
        self.nome = None
        self.id = None

    def em(self, base):
        return Cliente(base, self.cookies)

    def chamar(self, metodo, rota, dados=None, esperado=200, formulario=False):
        headers = {"Accept": "application/json"}
        if metodo not in {"GET", "HEAD"}:
            headers["X-CSRF-TOKEN"] = self.chamar("GET", "/backend/csrf")["token"]
        corpo = None
        if dados is not None:
            if formulario:
                headers["Content-Type"] = "application/x-www-form-urlencoded"
                corpo = urllib.parse.urlencode(dados).encode()
            else:
                headers["Content-Type"] = "application/json"
                corpo = json.dumps(dados, ensure_ascii=False).encode()
        req = urllib.request.Request(self.base + rota, data=corpo, headers=headers, method=metodo)
        try:
            resposta = self.http.open(req, timeout=8)
        except urllib.error.HTTPError as erro:
            resposta = erro
        with resposta:
            texto = resposta.read()
            if resposta.status != esperado:
                raise AssertionError(f"{metodo} {rota}: HTTP {resposta.status}, esperado {esperado}: {texto[:400]!r}")
            return json.loads(texto) if texto else None

    def criar_conta(self):
        self.nome = "Chat" + uuid.uuid4().hex[:12]
        email = self.nome.lower() + "@example.com"
        senha = "MensagemDemo1!"
        self.chamar("POST", "/backend/usuarios",
                    {"email": email, "nomeUsuario": self.nome, "senha": senha}, esperado=201)
        self.chamar("POST", "/backend/login", {"email": email, "senha": senha},
                    esperado=204, formulario=True)
        self.id = self.chamar("GET", "/backend/sessao")["id"]
        return self

    def dm(self, outro):
        self.chamar("PUT", f"/backend/pessoas/{outro.id}/seguimento", esperado=204)
        outro.chamar("PUT", f"/backend/pessoas/{self.id}/seguimento", esperado=204)
        return next(c["id"] for c in self.chamar("GET", "/backend/chat/conversas") if c["outroId"] == outro.id)

    def enviar(self, conversa, texto, id_mensagem=None, esperado=202):
        return self.chamar("POST", historico(conversa), {"id": id_mensagem or str(uuid.uuid4()), "texto": texto},
                           esperado=esperado)


def historico(conversa):
    return f"/backend/chat/conversas/{conversa}/mensagens"


def leituras(conversa):
    return f"/backend/chat/conversas/{conversa}/leituras"


def esperar(consulta, segundos=40):
    limite = time.monotonic() + segundos
    erro = None
    while time.monotonic() < limite:
        try:
            if resultado := consulta():
                return resultado
        except (OSError, AssertionError) as excecao:
            erro = excecao
        time.sleep(.35)
    raise AssertionError(f"A condição não ocorreu em {segundos}s") from erro
