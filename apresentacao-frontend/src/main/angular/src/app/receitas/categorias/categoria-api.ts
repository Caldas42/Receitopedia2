import { HttpClient, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map } from 'rxjs';

export interface Categoria {
  id: string;
  nome: string;
}

@Injectable({ providedIn: 'root' })
export class CategoriaApi {
  private readonly http = inject(HttpClient);
  private readonly url = '/backend/categorias';

  listar() {
    return this.http.get<Categoria[]>(this.url);
  }

  catalogo() {
    return this.http.get<Categoria[]>(this.url, { observe: 'response' }).pipe(map(lerCatalogo));
  }

  editar(id: string, nome: string) {
    return this.http.put<Categoria>(this.url + '/' + encodeURIComponent(id), { nome });
  }

  remover(id: string) {
    return this.http.delete<void>(this.url + '/' + encodeURIComponent(id));
  }

  reordenar(ids: string[], versao: number) {
    return this.http.put<Categoria[]>(this.url + '/ordem', { ids, versao }, { observe: 'response' })
      .pipe(map(lerCatalogo));
  }

  adicionar(nome: string) {
    return this.http.post<Categoria>(this.url, { nome });
  }
}

function lerCatalogo(resposta: HttpResponse<Categoria[]>) {
  return { categorias: resposta.body ?? [], versao: Number(resposta.headers.get('X-Versao-Categorias')) };
}
