import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { SessaoApi } from './sessao-api';

export const exigeSessao: CanActivateFn = () => {
  const sessao = inject(SessaoApi);
  const login = inject(Router).createUrlTree(['/entrar']);
  return sessao.consultar().pipe(
    map((usuario) => usuario ? true : login),
    catchError(() => of(login)),
  );
};

export const exigeAdmin: CanActivateFn = () => {
  const sessao = inject(SessaoApi);
  const inicio = inject(Router).createUrlTree(['/receitas']);
  return sessao.consultar().pipe(
    map((usuario) => usuario?.papel === 'ADMIN' ? true : inicio),
    catchError(() => of(inicio)),
  );
};
