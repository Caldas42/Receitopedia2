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
  fotoPrincipalId: string | null;
}

export interface Receita {
  id: string;
  versao: number;
  dados: Omit<DadosReceita, 'horas' | 'minutos'> & { tempoPreparo: { minutosTotais: number } | null };
  dono: { id: string; nomeUsuario: string } | null;
  fotos: { id: string }[];
}

export interface FotosParaSalvar {
  mantidas: string[];
  arquivos: File[];
  principal: number | null;
}

export function caminhoFoto(receitaId: string, fotoId: string): string {
  return '/backend/receitas/' + encodeURIComponent(receitaId) + '/fotos/' + encodeURIComponent(fotoId);
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

  adicionar(dados: DadosReceita, fotos: FotosParaSalvar) {
    return this.http.post<Receita>(this.url, this.formulario(dados, fotos));
  }

  editar(id: string, versao: number, dados: DadosReceita, fotos: FotosParaSalvar) {
    return this.http.put<Receita>(this.url + '/' + encodeURIComponent(id), this.formulario(dados, fotos, versao));
  }

  private formulario(dados: DadosReceita, fotos: FotosParaSalvar, versao?: number): FormData {
    const partes = new FormData();
    const cadastro = {
      ...dados,
      versao,
      fotosMantidas: fotos.mantidas,
      fotoPrincipal: fotos.principal,
    };
    partes.append('receita', new Blob([JSON.stringify(cadastro)], { type: 'application/json' }));
    for (const arquivo of fotos.arquivos) {
      partes.append('fotos', arquivo);
    }
    return partes;
  }

  remover(id: string, versao: number) {
    return this.http.delete<void>(this.url + '/' + encodeURIComponent(id), { params: { versao } });
  }
}

export function mensagemDeErro(erro: HttpErrorResponse, mensagemPadrao: string): string {
  const detalhe = erro.error?.detail;
  return [400, 403, 404, 409, 413].includes(erro.status) && typeof detalhe === 'string'
    ? detalhe
    : mensagemPadrao;
}
