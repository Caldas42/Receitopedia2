import { Routes } from '@angular/router';
import { ReceitaDetalhe } from './receitas/receita-detalhe/receita-detalhe';
import { ReceitaFormulario } from './receitas/receita-formulario/receita-formulario';
import { ReceitaLista } from './receitas/receita-lista/receita-lista';

export const routes: Routes = [
  { path: '', redirectTo: 'receitas', pathMatch: 'full' },
  { path: 'receitas', component: ReceitaLista, title: 'Receitas | Receitopédia' },
  { path: 'receitas/nova', component: ReceitaFormulario, title: 'Nova receita | Receitopédia' },
  { path: 'receitas/:id/editar', component: ReceitaFormulario, title: 'Editar receita | Receitopédia' },
  { path: 'receitas/:id', component: ReceitaDetalhe, title: 'Receita | Receitopédia' },
];
