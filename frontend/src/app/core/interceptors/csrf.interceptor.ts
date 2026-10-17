import { HttpErrorResponse, HttpInterceptorFn, HttpResponse } from "@angular/common/http";
import { inject } from "@angular/core";
import { catchError, switchMap, tap, throwError } from "rxjs";
import { CsrfTokenService } from "../services/csrf-token.service";

const sessionChanges = new Set([
  "/api/auth/login", "/api/auth/registro", "/api/usuarios/registro",
  "/api/users/registro", "/api/auth/verificar-email",
  "/api/auth/logout", "/api/auth/restablecer-password",
]);

// The session cookie remains HttpOnly. The CSRF token is kept only in memory.
export const csrfInterceptor: HttpInterceptorFn = (request, next) => {
  if (
    !request.url.startsWith("/api/") ||
    ["GET", "HEAD", "OPTIONS"].includes(request.method)
  )
    return next(request);
  const csrf = inject(CsrfTokenService);
  const path = request.url.split("?")[0];
  return csrf.getToken().pipe(
    switchMap(({ headerName, token }) => next(request.clone({
      setHeaders: { [headerName]: token },
    }))),
    tap((event) => {
      if (event instanceof HttpResponse && sessionChanges.has(path)) csrf.invalidate();
    }),
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 || error.status === 403) csrf.invalidate();
      // A mutation is never repeated automatically.
      return throwError(() => error);
    }),
  );
};
