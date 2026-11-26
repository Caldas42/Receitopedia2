import {
  afterNextRender, ChangeDetectorRef, Component, computed, DestroyRef, ElementRef, inject,
  Injector, OnInit, signal, viewChild,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { auditTime, Subject } from 'rxjs';
import { SessaoApi } from '../contas/sessao-api';
import { ChatApi, Conversa, Grupo, Mensagem } from './chat-api';

type Envio = { conversa: string; id: string; texto: string; arquivos: File[] };

@Component({
  selector: 'app-chat-pagina',
  imports: [FormsModule, DatePipe],
  templateUrl: './chat-pagina.html',
  styleUrl: './chat-pagina.css',
  host: { '(document:visibilitychange)': 'marcarVisiveis()' },
})
export class ChatPagina implements OnInit {
  protected readonly api = inject(ChatApi);
  private readonly injector = inject(Injector);
  private readonly destroy = inject(DestroyRef);
  private readonly detector = inject(ChangeDetectorRef);
  private readonly painel = viewChild<ElementRef<HTMLElement>>('historico');
  private readonly avisos = new Subject<void>();
  private readonly leiturasEmAndamento = new Set<string>();
  private readonly leiturasConfirmadas = new Set<string>();
  private tentativa?: Envio;
  private revisao = 0;

  protected readonly usuario = inject(SessaoApi).usuario;
  protected readonly conversas = signal<Conversa[]>([]);
  protected readonly id = signal<string | null>(null);
  protected readonly conversa = computed(() => this.conversas().find((item) => item.id === this.id()));
  protected readonly mensagens = signal<Mensagem[]>([]);
  protected readonly anteriores = signal(false);
  protected readonly grupo = signal<Grupo | null>(null);
  protected readonly admin = computed(() => this.grupo()?.membros.some((m) => m.id === this.usuario()?.id && m.admin));
  protected readonly erro = signal('');
  protected readonly online = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly carregandoAnterior = signal(false);

  protected novo: 'grupo' | null = null;
  protected nomeGrupo = '';
  protected nomesGrupo = '';
  protected alterarGrupo = false;
  protected novoNome = '';
  protected novoMembro = '';
  protected busca = '';
  protected termo = '';
  protected texto = '';
  protected arquivos: File[] = [];
  protected editando: Mensagem | null = null;
  protected textoEdicao = '';

  ngOnInit() {
    this.avisos.pipe(auditTime(100), takeUntilDestroyed(this.destroy)).subscribe(() => void this.sincronizar());
    const eventos = this.api.eventos();
    eventos.addEventListener('conectado', () => {
      this.online.set(true);
      this.avisos.next();
    });
    eventos.addEventListener('atualizacao', () => this.avisos.next());
    eventos.onerror = () => this.online.set(false);
    const revisao = window.setInterval(() => this.avisos.next(), 20_000);
    this.destroy.onDestroy(() => {
      eventos.close();
      window.clearInterval(revisao);
    });
    void this.sincronizar();
  }

  protected selecionar(id: string) {
    this.id.set(id);
    this.mensagens.set([]);
    this.anteriores.set(false);
    this.grupo.set(null);
    this.tentativa = undefined;
    this.alterarGrupo = false;
    this.editar(null);
    this.termo = this.busca = this.texto = '';
    this.arquivos = [];
    this.erro.set('');
    void this.carregarMensagens(true);
  }

  protected novaConversa() {
    void this.executar(async () => {
      const resposta = await this.api.criarGrupo(this.nomeGrupo, this.nomesGrupo.split(',').map((nome) => nome.trim()).filter(Boolean));
      this.novo = null;
      this.nomeGrupo = this.nomesGrupo = '';
      this.selecionar(resposta.id);
      await this.sincronizar();
    });
  }

  protected pesquisar() {
    this.termo = this.busca.trim();
    this.mensagens.set([]);
    this.anteriores.set(false);
    void this.carregarMensagens(true);
  }

  protected carregarAnteriores() {
    if (this.carregandoAnterior()) {
      return;
    }
    this.carregandoAnterior.set(true);
    void this.carregarMensagens(false, this.mensagens()[0]?.sequencia).finally(() => this.carregandoAnterior.set(false));
  }

  protected selecionarArquivos(evento: Event) {
    const entrada = evento.target as HTMLInputElement;
    const arquivos = Array.from(entrada.files ?? []);
    entrada.value = '';
    if (arquivos.length > 4 || arquivos.some((arquivo) => arquivo.size > 5 * 1024 * 1024)) {
      this.erro.set('Use até quatro imagens de até 5 MB cada.');
      return;
    }
    this.erro.set('');
    this.arquivos = arquivos;
  }

  protected enviar() {
    const conversa = this.id();
    if (!conversa || (!this.texto.trim() && !this.arquivos.length)) {
      return;
    }
    const anterior = this.tentativa;
    const envio = anterior?.conversa === conversa && anterior.texto === this.texto && anterior.arquivos === this.arquivos
      ? anterior : { conversa, id: novoUuid(), texto: this.texto, arquivos: this.arquivos };
    this.tentativa = envio;
    void this.executar(async () => {
      await this.api.enviar(conversa, envio.id, envio.texto, envio.arquivos);
      if (this.id() === conversa) {
        this.texto = '';
        this.arquivos = [];
        this.tentativa = undefined;
        await this.carregarMensagens(true);
      }
      await this.sincronizar();
    });
  }

  protected editar(mensagem: Mensagem | null) {
    this.editando = mensagem;
    this.textoEdicao = mensagem?.texto ?? '';
  }

  protected confirmarEdicao(conversa: string) {
    const mensagem = this.editando;
    if (!mensagem) {
      return;
    }
    void this.executar(async () => {
      await this.api.editar(conversa, mensagem, this.textoEdicao);
      this.editar(null);
      await this.sincronizar();
    });
  }

  protected apagar(conversa: string, mensagem: Mensagem) {
    if (window.confirm('Apagar esta mensagem?')) {
      void this.executar(async () => {
        await this.api.apagar(conversa, mensagem);
        await this.sincronizar();
      });
    }
  }

  protected podeAlterar(mensagem: Mensagem) {
    return mensagem.autorId === this.usuario()?.id && !mensagem.apagada
      && Date.now() - new Date(mensagem.enviadaEm).getTime() <= 900_000;
  }

  protected bloquear(conversa: Conversa) {
    if (conversa.outroId) {
      const alvo = conversa.outroId;
      this.gerir(() => this.api.bloquear(alvo, !conversa.bloqueioMeu));
    }
  }

  protected abrirGrupo(conversa: Conversa) {
    this.alterarGrupo = !this.alterarGrupo;
    this.novoNome = conversa.nome;
    this.novoMembro = '';
    if (this.alterarGrupo) {
      this.gerir(async () => {});
    }
  }

  protected gerir(operacao: () => Promise<void>) {
    void this.executar(async () => {
      await operacao();
      await this.sincronizar();
      this.detector.markForCheck();
    });
  }

  protected adicionarMembro(id: string) {
    this.gerir(async () => {
      await this.api.adicionarMembro(id, this.novoMembro);
      this.novoMembro = '';
    });
  }

  protected removerMembro(id: string, membro: string) {
    if (window.confirm('Remover este membro?')) {
      this.gerir(() => this.api.remover(id, membro));
    }
  }

  protected sair(id: string) {
    if (window.confirm('Sair do grupo?')) {
      this.gerir(() => this.api.sair(id));
    }
  }

  protected marcarVisiveis() {
    const conversa = this.id();
    const elemento = this.painel()?.nativeElement;
    if (!conversa || !elemento || document.visibilityState !== 'visible') {
      return;
    }
    const limites = elemento.getBoundingClientRect();
    const ids = Array.from(elemento.querySelectorAll<HTMLElement>('[data-leitura]')).flatMap((item) => {
      const id = item.dataset['leitura'];
      const caixa = item.getBoundingClientRect();
      return id && caixa.top < limites.bottom && caixa.bottom > limites.top
        && !this.leiturasConfirmadas.has(id) && !this.leiturasEmAndamento.has(id) ? [id] : [];
    }).slice(0, 30);
    if (!ids.length) {
      return;
    }
    ids.forEach((id) => this.leiturasEmAndamento.add(id));
    this.api.marcarLeituras(conversa, ids).then(() => ids.forEach((id) => this.leiturasConfirmadas.add(id)))
      .catch(() => { /* A próxima atualização da conversa tenta novamente. */ })
      .finally(() => ids.forEach((id) => this.leiturasEmAndamento.delete(id)));
  }

  private async sincronizar() {
    try {
      const lista = await this.api.conversas();
      this.conversas.set(lista);
      const id = this.id();
      const atual = lista.find((item) => item.id === id);
      if (id && !atual) {
        this.id.set(null);
        this.grupo.set(null);
        this.mensagens.set([]);
      } else if (atual) {
        await this.carregarMensagens(false);
        if (atual.tipo === 'GRUPO' && this.alterarGrupo && this.id() === atual.id) {
          const grupo = await this.api.grupo(atual.id);
          if (this.id() === atual.id) {
            this.grupo.set(grupo);
          }
        }
      }
    } catch {
      // SSE reconecta e relê o estado; a ação de usuário exibe a falha quando pertinente.
    }
  }

  private async carregarMensagens(rodape: boolean, antes?: number) {
    const id = this.id();
    if (!id) {
      return;
    }
    const revisao = ++this.revisao;
    const painel = this.painel()?.nativeElement;
    const seguir = !painel || painel.scrollHeight - painel.scrollTop - painel.clientHeight < 70;
    const altura = painel?.scrollHeight ?? 0;
    try {
      const novas = (await this.api.historico(id, this.termo, antes)).reverse();
      if (id !== this.id() || revisao !== this.revisao) {
        return;
      }
      if (antes !== undefined) {
        this.anteriores.set(novas.length === 30);
        this.mensagens.update((atuais) => [...novas, ...atuais]);
      } else {
        const antigas = this.mensagens().filter((antiga) => antiga.sequencia < (novas[0]?.sequencia ?? 0));
        this.mensagens.set([...antigas, ...novas]);
        if (!antigas.length) {
          this.anteriores.set(novas.length === 30);
        }
      }
      afterNextRender(() => {
        const elemento = this.painel()?.nativeElement;
        if (elemento && id === this.id()) {
          if (antes !== undefined) {
            elemento.scrollTop += elemento.scrollHeight - altura;
          } else if (rodape || seguir) {
            elemento.scrollTop = elemento.scrollHeight;
          }
          this.marcarVisiveis();
        }
      }, { injector: this.injector });
    } catch {
      if (id === this.id()) {
        this.erro.set('Não foi possível carregar o histórico.');
      }
    }
  }

  private async executar(operacao: () => Promise<void>) {
    if (this.ocupado()) {
      return;
    }
    this.ocupado.set(true);
    this.erro.set('');
    try {
      await operacao();
    } catch (erro: unknown) {
      const resposta = erro as { status?: number; error?: { detail?: string } };
      this.erro.set(resposta.error?.detail ?? 'Não foi possível concluir a operação. Tente novamente.');
      if (resposta.status === 409) {
        await this.sincronizar();
      }
    } finally {
      this.ocupado.set(false);
    }
  }
}

function novoUuid(): string {
  if (typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
  return [hex.slice(0, 8), hex.slice(8, 12), hex.slice(12, 16), hex.slice(16, 20), hex.slice(20)].join('-');
}
