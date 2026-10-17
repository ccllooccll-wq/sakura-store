import { HttpErrorResponse, HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { Router } from "@angular/router";
import { catchError, throwError } from "rxjs";
import { AuthService } from "../services/auth.service";

export const authErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const path = request.url.split("?")[0];
  return next(request).pipe(catchError((error: HttpErrorResponse) => {
    if (request.url.startsWith("/api/") && error.status === 401) {
      // Anonymous session checks and incorrect OTPs are handled by their own forms.
      if (!path.startsWith("/api/auth/") && path !== "/api/usuarios/registro" &&
          path !== "/api/users/registro") {
        auth.clearSession();
        if (!router.url.startsWith("/login")) void router.navigate(["/login"], {
          queryParams: { expired: "1" }, replaceUrl: true,
        });
      }
    }
    return throwError(() => error);
  }));
};
