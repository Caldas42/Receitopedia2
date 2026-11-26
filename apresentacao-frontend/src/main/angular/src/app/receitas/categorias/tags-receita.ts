import { Component, input } from '@angular/core';
import { Categoria } from './categoria-api';

@Component({
  selector: 'app-tags-receita',
  template: `
    @if (categorias().length) {
      <ul class="tags-receita list-unstyled" aria-label="Categorias">
        @for (categoria of categorias(); track categoria.id) {
          <li>{{ categoria.nome }}</li>
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

    .tags-receita li {
      border-radius: 1rem;
      padding: 0.2rem 0.65rem;
      background: #f1e7dc;
      color: #573c31;
      font-size: 0.85rem;
      overflow-wrap: anywhere;
    }
  `,
})
export class TagsReceita {
  readonly categorias = input.required<Categoria[]>();
}
