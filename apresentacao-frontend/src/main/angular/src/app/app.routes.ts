import { Routes } from '@angular/router';
import { ReceitaDetalhe } from './receitas/receita-detalhe/receita-detalhe';
import { ReceitaFormulario } from './receitas/receita-formulario/receita-formulario';
import { ReceitaLista } from './receitas/receita-lista/receita-lista';

import { exigeSessao } from './contas/sessao-guard';
import { ContaFormulario } from './contas/conta-formulario';

export const routes: Routes = [
  { path: '', redirectTo: 'receitas', pathMatch: 'full' },
  { path: 'entrar', component: ContaFormulario, title: 'Entrar | Receitopédia' },
  { path: 'cadastro', component: ContaFormulario, data: { cadastro: true }, title: 'Cadastro | Receitopédia' },
  { path: 'receitas', component: ReceitaLista, title: 'Receitas | Receitopédia' },
  { path: 'receitas/nova', component: ReceitaFormulario, canActivate: [exigeSessao], title: 'Nova receita | Receitopédia' },
  { path: 'receitas/:id/editar', component: ReceitaFormulario, canActivate: [exigeSessao], title: 'Editar receita | Receitopédia' },
  { path: 'receitas/:id', component: ReceitaDetalhe, title: 'Receita | Receitopédia' },
];
