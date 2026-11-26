import { HttpBackend, HttpClient, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { switchMap } from 'rxjs';

export const enviarCsrf: HttpInterceptorFn = (requisicao, proximo) => {
  if (!requisicao.url.startsWith('/backend/') || ['GET', 'HEAD', 'OPTIONS'].includes(requisicao.method)) {
    return proximo(requisicao);
  }

  const http = new HttpClient(inject(HttpBackend));
  return http.get<{ token: string }>('/backend/csrf').pipe(
    switchMap(({ token }) => proximo(requisicao.clone({ setHeaders: { 'X-CSRF-TOKEN': token } }))),
  );
};
