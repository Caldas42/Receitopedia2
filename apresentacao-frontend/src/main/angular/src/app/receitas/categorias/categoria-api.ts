import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

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

  adicionar(nome: string) {
    return this.http.post<Categoria>(this.url, { nome });
  }
}
