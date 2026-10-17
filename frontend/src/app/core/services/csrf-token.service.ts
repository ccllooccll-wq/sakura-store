import { HttpClient } from "@angular/common/http";
import { Injectable, inject } from "@angular/core";
import { Observable, finalize, of, shareReplay, tap } from "rxjs";
import { CsrfToken } from "../models/auth.model";

@Injectable({ providedIn: "root" })
export class CsrfTokenService {
  private readonly http = inject(HttpClient);
  private cached?: CsrfToken;
  private pending?: Observable<CsrfToken>;
  private generation = 0;

  getToken(): Observable<CsrfToken> {
    if (this.cached) return of(this.cached);
    if (this.pending) return this.pending;
    const generation = this.generation;
    this.pending = this.http.get<CsrfToken>("/api/auth/csrf").pipe(
      tap((token) => {
        if (generation === this.generation) this.cached = token;
      }),
      finalize(() => {
        if (generation === this.generation) this.pending = undefined;
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    return this.pending;
  }

  invalidate(): void {
    this.generation++;
    this.cached = undefined;
    this.pending = undefined;
  }
}
