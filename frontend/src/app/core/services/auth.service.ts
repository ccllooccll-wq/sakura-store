import { Injectable, inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { BehaviorSubject, Observable, catchError, of, tap } from "rxjs";
import { AuthResponse, User } from "../models/user.model";
import { RegisterRequest, ApiResponse } from "../models/auth.model";
import { CsrfTokenService } from "./csrf-token.service";
@Injectable({ providedIn: "root" })
export class AuthService {
  private readonly api = "/api";
  private readonly userSubject = new BehaviorSubject<User | null>(null);
  readonly currentUser$ = this.userSubject.asObservable();
  private readonly http = inject(HttpClient);
  private readonly csrf = inject(CsrfTokenService);

  clearSession(): void {
    this.userSubject.next(null);
    this.csrf.invalidate();
  }
  login(username: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.api}/auth/login`, {
      username,
      password,
    });
  }
  register(data: RegisterRequest): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(`${this.api}/usuarios/registro`, data);
  }
  verifyEmail(email: string, codigo: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.api}/auth/verificar-email`, { email, codigo })
      .pipe(tap((r) => this.userSubject.next(r.user ?? null)));
  }
  resendCode(email: string): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(`${this.api}/auth/reenviar-codigo`, {
      email,
    });
  }
  requestPasswordReset(email: string): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(
      `${this.api}/auth/solicitar-recuperacion`,
      { email },
    );
  }
  resetPassword(
    email: string,
    codigo: string,
    nuevaPassword: string,
  ): Observable<ApiResponse> {
    return this.http
      .post<ApiResponse>(`${this.api}/auth/restablecer-password`, {
        email,
        codigo,
        nuevaPassword,
      })
      .pipe(tap(() => this.clearSession()));
  }
  loadSession(): Observable<User | null> {
    return this.http.get<User>(`${this.api}/auth/me`).pipe(
      tap((u) => this.userSubject.next(u)),
      catchError(() => {
        this.clearSession();
        return of(null);
      }),
    );
  }
  logout(): Observable<void> {
    return this.http
      .post<void>(`${this.api}/auth/logout`, {})
      .pipe(tap(() => this.clearSession()));
  }
  getStoredUser(): User | null {
    return this.userSubject.value;
  }
  isLoggedIn(): boolean {
    return this.userSubject.value !== null;
  }
  isAdmin(): boolean {
    return this.userSubject.value?.roleName === "ROLE_ADMIN";
  }
  canManageInventory(): boolean {
    return this.isAdmin();
  }
}
