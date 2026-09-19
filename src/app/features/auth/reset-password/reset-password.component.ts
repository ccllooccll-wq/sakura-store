import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-reset-password',
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
          <h2>Recuperar Contraseña</h2>
          <p class="subtitle" *ngIf="step === 1">
            Introduce tu correo registrado para recibir un código OTP de 6 dígitos.
          </p>
          <p class="subtitle" *ngIf="step === 2">
            Ingresa el código OTP de 6 dígitos enviado a <strong class="email-highlight">{{ email }}</strong> y tu nueva contraseña.
          </p>
        </div>

        <!-- Alertas de estado -->
        <div *ngIf="successMessage" class="alert alert-success">
          <i class="fa-solid fa-circle-check icon-lg"></i>
          <div>
            <strong>¡Éxito!</strong>
            <p>{{ successMessage }}</p>
          </div>
        </div>

        <div *ngIf="errorMessage" class="alert alert-error">
          <i class="fa-solid fa-circle-exclamation icon-lg"></i>
          <div>
            <strong>Atención:</strong>
            <p>{{ errorMessage }}</p>
          </div>
        </div>

        <div *ngIf="infoMessage" class="alert alert-info">
          <i class="fa-solid fa-paper-plane icon-lg"></i>
          <div>
            <strong>Mensaje:</strong>
            <p>{{ infoMessage }}</p>
          </div>
        </div>

        <!-- Paso 1: Solicitar Código OTP -->
        <form *ngIf="step === 1" (ngSubmit)="onRequestCode()" #step1Form="ngForm">
          <div class="form-group">
            <label for="email">Correo Electrónico Registrado *</label>
            <div class="input-with-icon">
              <i class="fa-solid fa-envelope icon"></i>
              <input
                type="email"
                id="email"
                name="email"
                class="form-control"
                [(ngModel)]="email"
                required
                placeholder="ejemplo@gmail.com"
              />
            </div>
          </div>

          <button type="submit" class="btn-sakura btn-full" [disabled]="loading || !step1Form.valid">
            <span *ngIf="!loading"><i class="fa-solid fa-paper-plane"></i> Enviar Código OTP</span>
            <span *ngIf="loading"><i class="fa-solid fa-spinner fa-spin"></i> Enviando...</span>
          </button>
        </form>

        <!-- Paso 2: Introducir Código OTP + Nueva Contraseña -->
        <form *ngIf="step === 2 && !passwordResetSuccess" (ngSubmit)="onResetPassword()" #step2Form="ngForm">
          <div class="form-group">
            <label for="code">Código OTP (6 dígitos) *</label>
            <div class="otp-input-container">
              <input
                type="text"
                id="code"
                name="code"
                class="form-control otp-input"
                [(ngModel)]="codigo"
                required
                maxlength="6"
                pattern="^[0-9]{6}$"
                placeholder="483921"
                autocomplete="off"
              />
            </div>
          </div>

          <div class="form-group">
            <label for="nuevaPassword">Nueva Contraseña *</label>
            <div class="input-with-icon">
              <i class="fa-solid fa-lock icon"></i>
              <input
                type="password"
                id="nuevaPassword"
                name="nuevaPassword"
                class="form-control"
                [(ngModel)]="nuevaPassword"
                required
                minlength="6"
                placeholder="••••••••"
              />
            </div>
          </div>

          <div class="form-group">
            <label for="confirmPassword">Confirmar Nueva Contraseña *</label>
            <div class="input-with-icon">
              <i class="fa-solid fa-lock icon"></i>
              <input
                type="password"
                id="confirmPassword"
                name="confirmPassword"
                class="form-control"
                [(ngModel)]="confirmPassword"
                required
                placeholder="••••••••"
              />
            </div>
          </div>

          <button
            type="submit"
            class="btn-sakura btn-full"
            [disabled]="loading || !step2Form.valid || codigo.length !== 6 || nuevaPassword !== confirmPassword"
          >
            <span *ngIf="!loading"><i class="fa-solid fa-key"></i> Restablecer Contraseña</span>
            <span *ngIf="loading"><i class="fa-solid fa-spinner fa-spin"></i> Guardando...</span>
          </button>

          <div class="resend-section">
            <button
              type="button"
              class="btn-outline-sakura"
              (click)="step = 1"
              [disabled]="loading"
            >
              <i class="fa-solid fa-arrow-left"></i> Modificar correo o reenviar
            </button>
          </div>
        </form>

        <!-- Botón de éxito para ir al login -->
        <div *ngIf="passwordResetSuccess" class="action-success" style="margin-top: 20px;">
          <button routerLink="/login" class="btn-sakura btn-full">
            <i class="fa-solid fa-right-to-bracket"></i> Ir al Inicio de Sesión
          </button>
        </div>

        <div class="back-link-container" style="text-align: center; margin-top: 24px;">
          <a routerLink="/login" style="color: #94a3b8; font-size: 0.85rem; text-decoration: none;">
            <i class="fa-solid fa-arrow-left"></i> Volver al Login
          </a>
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
      max-width: 480px;
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
      color: #ff75a0;
      margin-bottom: 12px;
    }

    .auth-header h2 {
      font-size: 1.8rem;
      margin-bottom: 8px;
    }

    .subtitle {
      font-size: 0.9rem;
      color: #94a3b8;
      line-height: 1.5;
    }

    .email-highlight {
      color: #ff75a0;
      word-break: break-all;
    }

    .form-group {
      margin-bottom: 20px;
    }

    .form-group label {
      display: block;
      margin-bottom: 8px;
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
    }

    .otp-input {
      font-size: 1.5rem !important;
      letter-spacing: 10px;
      text-align: center;
      font-weight: bold;
      color: #ff75a0 !important;
      padding: 12px !important;
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

    .btn-outline-sakura {
      background: transparent;
      border: 1px solid rgba(255, 117, 160, 0.4);
      color: #ff75a0;
      padding: 10px 18px;
      border-radius: 8px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
      display: inline-flex;
      align-items: center;
      gap: 8px;
      margin-top: 10px;
    }

    .btn-full {
      width: 100%;
      justify-content: center;
      padding: 14px;
      margin-top: 10px;
      font-size: 1rem;
    }

    .alert {
      padding: 14px 16px;
      border-radius: 10px;
      margin-bottom: 20px;
      font-size: 0.88rem;
      display: flex;
      align-items: flex-start;
      gap: 12px;
      line-height: 1.4;
    }

    .alert p {
      margin: 4px 0 0 0;
    }

    .icon-lg {
      font-size: 1.4rem;
      margin-top: 2px;
    }

    .alert-success { background: rgba(46, 213, 115, 0.15); border: 1px solid rgba(46, 213, 115, 0.3); color: #2ed573; }
    .alert-error { background: rgba(255, 85, 85, 0.15); border: 1px solid rgba(255, 85, 85, 0.3); color: #ff5555; }
    .alert-info { background: rgba(52, 152, 219, 0.15); border: 1px solid rgba(52, 152, 219, 0.3); color: #3498db; }

    .resend-section {
      margin-top: 20px;
      text-align: center;
    }
  `]
})
export class ResetPasswordComponent {
  step = 1;
  email = '';
  codigo = '';
  nuevaPassword = '';
  confirmPassword = '';

  loading = false;
  passwordResetSuccess = false;

  errorMessage = '';
  successMessage = '';
  infoMessage = '';

  constructor(private authService: AuthService, private router: Router) {}

  onRequestCode(): void {
    if (!this.email) return;

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.infoMessage = '';

    this.authService.requestPasswordReset(this.email).subscribe({
      next: (res) => {
        this.loading = false;
        this.step = 2;
        this.infoMessage = res.mensaje || 'Se ha enviado un código de verificación a tu correo.';
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Error al solicitar el código. Verifica tu correo e intenta nuevamente.';
      }
    });
  }

  onResetPassword(): void {
    if (!this.email || !this.codigo || !this.nuevaPassword) return;

    if (this.nuevaPassword !== this.confirmPassword) {
      this.errorMessage = 'Las contraseñas no coinciden.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.infoMessage = '';

    this.authService.resetPassword(this.email, this.codigo, this.nuevaPassword).subscribe({
      next: (res) => {
        this.loading = false;
        this.passwordResetSuccess = true;
        this.successMessage = res.mensaje || '¡Contraseña restablecida exitosamente!';
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Error al restablecer contraseña. Revisa el código de 6 dígitos.';
      }
    });
  }
}
