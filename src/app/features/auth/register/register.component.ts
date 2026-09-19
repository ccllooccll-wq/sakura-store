import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="auth-wrapper">
      <div class="sakura-bg-glow"></div>

      <div class="glass-card auth-card">
        <div class="auth-header">
          <div class="brand-badge">
            <span class="flower">🌸</span>
            <span>SAKURA STORE</span>
          </div>
          <h2>Crear Cuenta</h2>
          <p class="subtitle">Regístrate con tu correo real para activar tu cuenta</p>
        </div>

        <div *ngIf="errorMessage" class="alert alert-error">
          <i class="fa-solid fa-circle-exclamation"></i>
          <span>{{ errorMessage }}</span>
        </div>

        <form (ngSubmit)="onSubmit()" #registerForm="ngForm">
          <div class="form-group">
            <label for="nombre">Nombre Completo</label>
            <div class="input-with-icon">
              <i class="fa-solid fa-id-card icon"></i>
              <input
                type="text"
                id="nombre"
                name="nombre"
                class="form-control"
                [(ngModel)]="nombre"
                required
                placeholder="Ej. Álvaro"
              />
            </div>
          </div>

          <div class="form-group">
            <label for="username">Nombre de Usuario</label>
            <div class="input-with-icon">
              <i class="fa-solid fa-user icon"></i>
              <input
                type="text"
                id="username"
                name="username"
                class="form-control"
                [(ngModel)]="username"
                required
                minlength="3"
                placeholder="Ej. alvaro123"
              />
            </div>
          </div>

          <div class="form-group">
            <label for="email">Correo Electrónico Real</label>
            <div class="input-with-icon">
              <i class="fa-solid fa-envelope icon"></i>
              <input
                type="email"
                id="email"
                name="email"
                class="form-control"
                [(ngModel)]="email"
                required
                email
                placeholder="usuario@gmail.com"
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
                minlength="6"
                placeholder="••••••••"
              />
            </div>
          </div>

          <button type="submit" class="btn-sakura btn-full" [disabled]="loading || !registerForm.valid">
            <span *ngIf="!loading"><i class="fa-solid fa-user-plus"></i> Registrarse</span>
            <span *ngIf="loading"><i class="fa-solid fa-spinner fa-spin"></i> Registrando...</span>
          </button>
        </form>

        <div class="auth-footer">
          <p>¿Ya tienes una cuenta? <a routerLink="/login" class="link-sakura">Inicia sesión</a></p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .auth-wrapper {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
      padding: 20px;
    }

    .sakura-bg-glow {
      position: absolute;
      width: 440px;
      height: 440px;
      background: radial-gradient(circle, rgba(255, 117, 160, 0.25) 0%, rgba(189, 147, 249, 0.1) 60%, transparent 100%);
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      filter: blur(50px);
      pointer-events: none;
    }

    .auth-card {
      width: 100%;
      max-width: 460px;
      padding: 40px;
      position: relative;
      z-index: 10;
    }

    .auth-header {
      text-align: center;
      margin-bottom: 24px;
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
      color: var(--sakura-pink, #ff75a0);
      margin-bottom: 12px;
    }

    .auth-header h2 {
      font-size: 1.8rem;
      margin-bottom: 6px;
    }

    .subtitle {
      font-size: 0.85rem;
      color: #94a3b8;
    }

    .form-group {
      margin-bottom: 18px;
    }

    .form-group label {
      display: block;
      margin-bottom: 6px;
      font-size: 0.85rem;
      font-weight: 600;
      color: #cbd5e1;
    }

    .input-with-icon {
      position: relative;
    }

    .input-with-icon .icon {
      position: absolute;
      left: 14px;
      top: 50%;
      transform: translateY(-50%);
      color: #64748b;
    }

    .input-with-icon .form-control {
      width: 100%;
      padding: 12px 14px 12px 42px;
      background: rgba(15, 23, 42, 0.6);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 8px;
      color: #ffffff;
      font-size: 0.95rem;
      box-sizing: border-box;
      transition: all 0.2s ease;
    }

    .input-with-icon .form-control:focus {
      outline: none;
      border-color: #ff75a0;
      box-shadow: 0 0 0 3px rgba(255, 117, 160, 0.2);
    }

    .btn-sakura {
      background: linear-gradient(135deg, #ff75a0 0%, #ff527b 100%);
      color: white;
      border: none;
      border-radius: 8px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
      display: inline-flex;
      align-items: center;
      gap: 8px;
    }

    .btn-sakura:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 4px 15px rgba(255, 117, 160, 0.4);
    }

    .btn-sakura:disabled {
      opacity: 0.6;
      cursor: not-allowed;
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
      border-radius: 8px;
      margin-bottom: 20px;
      font-size: 0.85rem;
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .auth-footer {
      margin-top: 24px;
      padding-top: 18px;
      border-top: 1px dashed rgba(255, 255, 255, 0.1);
      text-align: center;
      font-size: 0.85rem;
      color: #94a3b8;
    }

    .link-sakura {
      color: #ff75a0;
      text-decoration: none;
      font-weight: 600;
    }

    .link-sakura:hover {
      text-decoration: underline;
    }
  `]
})
export class RegisterComponent {
  nombre = '';
  username = '';
  email = '';
  password = '';
  loading = false;
  errorMessage = '';

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit() {
    if (!this.nombre || !this.username || !this.email || !this.password) return;

    this.loading = true;
    this.errorMessage = '';

    this.authService.register({
      nombre: this.nombre,
      username: this.username,
      email: this.email,
      password: this.password
    }).subscribe({
      next: (res) => {
        this.loading = false;
        // Redirect to verify email with email param
        this.router.navigate(['/verificar-email'], { queryParams: { email: this.email } });
      },
      error: (err) => {
        this.loading = false;
        if (err.error && err.error.message) {
          this.errorMessage = err.error.message;
        } else {
          this.errorMessage = 'Ocurrió un error al procesar el registro. Intenta de nuevo.';
        }
      }
    });
  }
}
