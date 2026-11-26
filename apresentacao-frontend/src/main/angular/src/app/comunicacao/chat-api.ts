import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface Conversa {
  id: string;
  tipo: 'DM' | 'GRUPO';
  nome: string;
  outroId: string | null;
  bloqueada: boolean;
  bloqueioMeu: boolean;
  ultimaMensagem: string | null;
  naoLidas: number;
}

export interface Mensagem {
  id: string;
  sequencia: number;
  autorId: string;
  autor: string;
  texto: string;
  versao: number;
  enviadaEm: string;
  editada: boolean;
  apagada: boolean;
  status: 'ENVIADA' | 'ENTREGUE' | 'LIDA';
  anexos: { id: string; tipo: string; miniatura: boolean }[];
}

export interface Grupo {
  id: string;
  nome: string;
  membros: { id: string; nome: string; admin: boolean }[];
}

const raiz = '/backend/chat';

@Injectable({ providedIn: 'root' })
export class ChatApi {
  private readonly http = inject(HttpClient);

  conversas() {
    return firstValueFrom(this.http.get<Conversa[]>(raiz + '/conversas'));
  }

  abrir(usuario: string) {
    return firstValueFrom(this.http.post<{ id: string }>(raiz + '/conversas/dm', { usuario }));
  }

  criarGrupo(nome: string, usuarios: string[]) {
    return firstValueFrom(this.http.post<{ id: string }>(raiz + '/conversas/grupos', { nome, usuarios }));
  }

  grupo(conversa: string) {
    return firstValueFrom(this.http.get<Grupo>(this.rota(conversa, '/grupo')));
  }

  renomear(conversa: string, nome: string) {
    return firstValueFrom(this.http.put<void>(this.rota(conversa, '/grupo'), { nome }));
  }

  adicionarMembro(conversa: string, usuario: string) {
    return firstValueFrom(this.http.post<void>(this.rota(conversa, '/grupo/membros'), { usuario }));
  }

  promover(conversa: string, id: string) {
    return firstValueFrom(this.http.put<void>(this.rota(conversa, '/grupo/membros/' + id + '/admin'), null));
  }

  remover(conversa: string, id: string) {
    return firstValueFrom(this.http.delete<void>(this.rota(conversa, '/grupo/membros/' + id)));
  }

  sair(conversa: string) {
    return firstValueFrom(this.http.delete<void>(this.rota(conversa, '/grupo/eu')));
  }

  bloquear(id: string, bloquear: boolean) {
    const caminho = raiz + '/bloqueios/' + id;
    return firstValueFrom(bloquear ? this.http.put<void>(caminho, null) : this.http.delete<void>(caminho));
  }

  historico(conversa: string, busca: string, antes?: number) {
    const params: Record<string, string | number> = { busca };
    if (antes !== undefined) {
      params['antes'] = antes;
    }
    return firstValueFrom(this.http.get<Mensagem[]>(this.rota(conversa, '/mensagens'), { params }));
  }

  marcarLeituras(conversa: string, ids: string[]) {
    return firstValueFrom(this.http.post<void>(this.rota(conversa, '/leituras'), { ids }));
  }

  enviar(conversa: string, id: string, texto: string, arquivos: File[]) {
    let corpo: FormData | { id: string; texto: string } = { id, texto };
    if (arquivos.length) {
      const form = new FormData();
      form.append('mensagem', new Blob([JSON.stringify(corpo)], { type: 'application/json' }));
      arquivos.forEach((arquivo) => form.append('anexos', arquivo));
      corpo = form;
    }
    return firstValueFrom(this.http.post<Mensagem>(this.rota(conversa, '/mensagens'), corpo));
  }

  editar(conversa: string, mensagem: Mensagem, texto: string) {
    return firstValueFrom(this.http.put<Mensagem>(this.rota(conversa, '/mensagens/' + mensagem.id),
      { texto, versao: mensagem.versao }));
  }

  apagar(conversa: string, mensagem: Mensagem) {
    return firstValueFrom(this.http.delete<Mensagem>(this.rota(conversa, '/mensagens/' + mensagem.id),
      { params: { versao: mensagem.versao } }));
  }

  anexo(conversa: string, id: string, miniatura = false) {
    return this.rota(conversa, '/anexos/' + id) + (miniatura ? '?miniatura=true' : '');
  }

  eventos() {
    return new EventSource(raiz + '/eventos');
  }

  private rota(conversa: string, sufixo: string) {
    return raiz + '/conversas/' + encodeURIComponent(conversa) + sufixo;
  }
}
