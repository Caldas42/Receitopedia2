import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Categoria } from './categoria-api';

@Component({
  selector: 'app-tags-receita',
  imports: [RouterLink],
  template: `
    @if (categorias().length) {
      <ul class="tags-receita list-unstyled" aria-label="Categorias">
        @for (categoria of categorias(); track categoria.id) {
          <li><a [routerLink]="destino()" [queryParams]="{ busca: categoria.nome }">{{ categoria.nome }}</a></li>
        }
      </ul>
    }
  `,
  styles: `
    .tags-receita {
      display: flex;
      flex-wrap: wrap;
      gap: 0.35rem;
      margin-block: 0.55rem;
    }

    .tags-receita a {
      display: inline-block;
      max-width: 100%;
      border-radius: 1rem;
      padding: 0.2rem 0.65rem;
      background: #f1e7dc;
      color: #573c31;
      font-size: 0.85rem;
      overflow-wrap: anywhere;
      text-decoration: none;
    }

    .tags-receita a:hover,
    .tags-receita a:focus-visible {
      text-decoration: underline;
    }
  `,
})
export class TagsReceita {
  readonly categorias = input.required<Categoria[]>();
  readonly destino = input('/receitas');
}
