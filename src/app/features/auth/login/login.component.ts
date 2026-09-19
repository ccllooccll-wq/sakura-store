import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="login-wrapper">
      <div class="sakura-bg-glow"></div>
      
      <div class="glass-card login-card">
        <div class="login-header">
          <div class="brand-badge">
            <span class="flower">🌸</span>
            <span>SAKURA STORE</span>
          </div>
          <h2>Iniciar Sesión</h2>
          <p class="subtitle">Sistema de Gestión de Inventario & Ventas</p>
        </div>

        <div *ngIf="errorMessage" class="alert alert-error">
          <i class="fa-solid fa-circle-exclamation"></i>
          <span>{{ errorMessage }}</span>
        </div>

        <form (ngSubmit)="onSubmit()" #loginForm="ngForm">
          <div class="form-group">
            <label for="username">Usuario</label>
            <div class="input-with-icon">
              <i class="fa-solid fa-user icon"></i>
              <input
                type="text"
                id="username"
                name="username"
                class="form-control"
                [(ngModel)]="username"
                required
                placeholder="Ej. admin"
              />
            </div>
          </div>

          <div class="form-group">
            <label for="password">Contraseña</label>
            <div class="input-with-icon">
              <i class="fa-solid fa-lock icon"></i>
              <input
                type="password"
                id="password"
                name="password"
                class="form-control"
                [(ngModel)]="password"
                required
                placeholder="••••••••"
              />
            </div>
            <div style="text-align: right; margin-top: 6px; font-size: 0.8rem;">
              <a routerLink="/recuperar-password" style="color: #8be9fd; text-decoration: none;">¿Olvidaste tu contraseña?</a>
            </div>
          </div>

          <button type="submit" class="btn-sakura btn-full" [disabled]="loading || !loginForm.valid">
            <span *ngIf="!loading">Ingresar al Sistema</span>
            <span *ngIf="loading"><i class="fa-solid fa-spinner fa-spin"></i> Autenticando...</span>
          </button>
        </form>

        <div class="register-link-container" style="text-align: center; margin-top: 16px; font-size: 0.85rem; color: #94a3b8;">
          ¿No tienes una cuenta? <a routerLink="/registro" style="color: #ff75a0; font-weight: 600; text-decoration: none;">Crear una nueva cuenta</a>
        </div>

        <div class="demo-credentials">
          <p><i class="fa-solid fa-key"></i> <strong>Credenciales Iniciales:</strong></p>
          <small>Usuario: <code>admin</code> | Clave: <code>admin123</code></small>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .login-wrapper {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
      padding: 20px;
    }

    .sakura-bg-glow {
      position: absolute;
      width: 400px;
      height: 400px;
      background: radial-gradient(circle, rgba(255, 117, 160, 0.25) 0%, rgba(189, 147, 249, 0.1) 60%, transparent 100%);
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      filter: blur(50px);
      pointer-events: none;
    }

    .login-card {
      width: 100%;
      max-width: 440px;
      padding: 40px;
      position: relative;
      z-index: 10;
    }

    .login-header {
      text-align: center;
      margin-bottom: 28px;
    }

    .brand-badge {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 6px 14px;
      background: rgba(255, 117, 160, 0.1);
      border: 1px solid rgba(255, 117, 160, 0.25);
      border-radius: 20px;
      font-size: 0.8rem;
      font-weight: 700;
      color: var(--sakura-pink);
      margin-bottom: 12px;
    }

    .login-header h2 {
      font-size: 1.8rem;
      margin-bottom: 6px;
    }

    .subtitle {
      font-size: 0.85rem;
      color: var(--text-secondary);
    }

    .input-with-icon {
      position: relative;
    }

    .input-with-icon .icon {
      position: absolute;
      left: 14px;
      top: 50%;
      transform: translateY(-50%);
      color: var(--text-muted);
    }

    .input-with-icon .form-control {
      padding-left: 42px;
    }

    .btn-full {
      width: 100%;
      justify-content: center;
      padding: 14px;
      margin-top: 12px;
      font-size: 1rem;
    }

    .alert-error {
      background: rgba(255, 85, 85, 0.15);
      border: 1px solid rgba(255, 85, 85, 0.3);
      color: #ff5555;
      padding: 12px;
      border-radius: var(--radius-sm);
      margin-bottom: 20px;
      font-size: 0.85rem;
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .demo-credentials {
      margin-top: 24px;
      padding-top: 18px;
      border-top: 1px dashed rgba(255, 255, 255, 0.1);
      text-align: center;
      font-size: 0.8rem;
      color: var(--text-secondary);
    }

    .demo-credentials code {
      background: rgba(255, 255, 255, 0.1);
      padding: 2px 6px;
      border-radius: 4px;
      color: var(--accent-amber);
    }
  `]
})
export class LoginComponent {
  username = '';
  password = '';
  loading = false;
  errorMessage = '';

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit() {
    if (!this.username || !this.password) return;

    this.loading = true;
    this.errorMessage = '';

    this.authService.login(this.username, this.password).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.requiresOtp && res.email) {
          this.router.navigate(['/verificar-email'], { queryParams: { email: res.email } });
        } else {
          this.router.navigate(['/users']);
        }
      },
      error: (err) => {
        this.loading = false;
        if (err.error && err.error.message) {
          this.errorMessage = err.error.message;
        } else {
          this.errorMessage = 'Credenciales incorrectas o error de servidor.';
        }
      }
    });
  }
}
