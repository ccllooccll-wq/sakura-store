import { ApplicationConfig } from "@angular/core";
import { provideRouter } from "@angular/router";
import { provideHttpClient, withInterceptors } from "@angular/common/http";

import { csrfInterceptor } from "./core/interceptors/csrf.interceptor";
import { authErrorInterceptor } from "./core/interceptors/auth-error.interceptor";
import { routes } from "./app.routes";

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([authErrorInterceptor, csrfInterceptor])),
  ],
};
