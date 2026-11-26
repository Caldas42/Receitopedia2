import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

export interface DadosReceita {
  titulo: string;
  descricao: string;
  ingredientes: string[];
  etapas: string[];
  horas: number | null;
  minutos: number | null;
}

export function formatarTempo(minutosTotais: number | null): string | null {
  if (minutosTotais === null) {
    return null;
  }
  const horas = Math.floor(minutosTotais / 60);
  const minutos = minutosTotais % 60;
  return [horas ? horas + 'h' : '', minutos || !horas ? minutos + 'min' : ''].filter(Boolean).join(' ');
}

export interface ReceitaResumo {
  id: string;
  titulo: string;
  descricao: string;
  tempoMinutos: number | null;
}

export interface Receita {
  id: string;
  versao: number;
  dados: Omit<DadosReceita, 'horas' | 'minutos'> & { tempoPreparo: { minutosTotais: number } | null };
  dono: { id: string; nomeUsuario: string } | null;
}

@Injectable({ providedIn: 'root' })
export class ReceitaApi {
  private readonly http = inject(HttpClient);
  private readonly url = '/backend/receitas';

  listar() {
    return this.http.get<ReceitaResumo[]>(this.url);
  }

  listarMinhas() {
    return this.http.get<ReceitaResumo[]>(this.url + '/minhas');
  }

  visualizar(id: string) {
    return this.http.get<Receita>(this.url + '/' + encodeURIComponent(id));
  }

  adicionar(dados: DadosReceita) {
    return this.http.post<Receita>(this.url, dados);
  }

  editar(id: string, versao: number, dados: DadosReceita) {
    return this.http.put<Receita>(this.url + '/' + encodeURIComponent(id), { ...dados, versao });
  }

  remover(id: string, versao: number) {
    return this.http.delete<void>(this.url + '/' + encodeURIComponent(id), { params: { versao } });
  }
}

export function mensagemDeErro(erro: HttpErrorResponse, mensagemPadrao: string): string {
  const detalhe = erro.error?.detail;
  return [400, 404, 409].includes(erro.status) && typeof detalhe === 'string'
    ? detalhe
    : mensagemPadrao;
}
