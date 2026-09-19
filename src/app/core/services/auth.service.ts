import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { AuthResponse, User } from '../models/user.model';

export interface RegisterRequest {
  nombre: string;
  username: string;
  email: string;
  password: string;
}

export interface ApiResponse {
  mensaje: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api';
  private currentUserSubject = new BehaviorSubject<User | null>(this.getStoredUser());
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {}

  login(username: string, password: String): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, { username, password }).pipe(
      tap(res => {
        if (res.token && res.user) {
          localStorage.setItem('sakura_token', res.token);
          localStorage.setItem('sakura_user', JSON.stringify(res.user));
          this.currentUserSubject.next(res.user);
        }
      })
    );
  }

  register(data: RegisterRequest): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(`${this.apiUrl}/usuarios/registro`, data);
  }

  verifyEmail(email: string, codigo: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/verificar-email`, { email, codigo }).pipe(
      tap(res => {
        if (res.token && res.user) {
          localStorage.setItem('sakura_token', res.token);
          localStorage.setItem('sakura_user', JSON.stringify(res.user));
          this.currentUserSubject.next(res.user);
        }
      })
    );
  }

  resendCode(email: string): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(`${this.apiUrl}/auth/reenviar-codigo`, { email });
  }

  requestPasswordReset(email: string): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(`${this.apiUrl}/auth/solicitar-recuperacion`, { email });
  }

  resetPassword(email: string, codigo: string, nuevaPassword: string): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(`${this.apiUrl}/auth/restablecer-password`, { email, codigo, nuevaPassword });
  }

  logout(): void {
    localStorage.removeItem('sakura_token');
    localStorage.removeItem('sakura_user');
    this.currentUserSubject.next(null);
  }

  getToken(): string | null {
    return localStorage.getItem('sakura_token');
  }

  getStoredUser(): User | null {
    const userJson = localStorage.getItem('sakura_user');
    return userJson ? JSON.parse(userJson) : null;
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  isAdmin(): boolean {
    const user = this.currentUserSubject.value;
    return user?.roleName === 'ROLE_ADMIN';
  }
}
