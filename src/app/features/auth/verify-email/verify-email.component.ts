import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-verify-email',
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
          <h2>Verifica tu correo</h2>
          <p class="subtitle" *ngIf="!verified">
            Hemos enviado un código de 6 dígitos a:<br>
            <strong class="email-highlight">{{ email || 'tu correo registrado' }}</strong>
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

        <!-- Formulario de verificación (si no está aún verificado) -->
        <div *ngIf="!verified">
          <div class="form-group" *ngIf="!hasPresetEmail">
            <label for="emailInput">Correo Electrónico</label>
            <div class="input-with-icon">
              <i class="fa-solid fa-envelope icon"></i>
              <input
                type="email"
                id="emailInput"
                class="form-control"
                [(ngModel)]="email"
                placeholder="usuario@gmail.com"
                required
              />
            </div>
          </div>

          <form (ngSubmit)="onVerify()" #otpForm="ngForm">
            <div class="form-group">
              <label for="code">Código de Verificación (6 dígitos)</label>
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
                  (keyup.enter)="onVerify()"
                />
              </div>
              <small class="help-text">Introduce el código numérico de 6 dígitos recibido en tu bandeja de entrada.</small>
            </div>

            <button
              type="submit"
              class="btn-sakura btn-full"
              [disabled]="loading || !codigo || codigo.length !== 6 || !email"
            >
              <span *ngIf="!loading"><i class="fa-solid fa-shield-check"></i> Verificar Código</span>
              <span *ngIf="loading"><i class="fa-solid fa-spinner fa-spin"></i> Validando...</span>
            </button>
          </form>

          <div class="resend-section">
            <p>¿No recibiste el correo o el código expiró?</p>
            <button
              type="button"
              class="btn-outline-sakura"
              (click)="onResend()"
              [disabled]="resendLoading || !email"
            >
              <span *ngIf="!resendLoading"><i class="fa-solid fa-rotate-right"></i> Reenviar código</span>
              <span *ngIf="resendLoading"><i class="fa-solid fa-spinner fa-spin"></i> Reenviando...</span>
            </button>
          </div>
        </div>

        <!-- Botón para ir a login si fue exitoso -->
        <div *ngIf="verified" class="action-success">
          <button routerLink="/login" class="btn-sakura btn-full">
            <i class="fa-solid fa-right-to-bracket"></i> Iniciar Sesión Ahora
          </button>
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

    .help-text {
      display: block;
      margin-top: 6px;
      font-size: 0.78rem;
      color: #64748b;
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

    .btn-outline-sakura:hover:not(:disabled) {
      background: rgba(255, 117, 160, 0.1);
      border-color: #ff75a0;
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

    .alert-success {
      background: rgba(46, 213, 115, 0.15);
      border: 1px solid rgba(46, 213, 115, 0.3);
      color: #2ed573;
    }

    .alert-error {
      background: rgba(255, 85, 85, 0.15);
      border: 1px solid rgba(255, 85, 85, 0.3);
      color: #ff5555;
    }

    .alert-info {
      background: rgba(52, 152, 219, 0.15);
      border: 1px solid rgba(52, 152, 219, 0.3);
      color: #3498db;
    }

    .resend-section {
      margin-top: 28px;
      padding-top: 20px;
      border-top: 1px dashed rgba(255, 255, 255, 0.1);
      text-align: center;
      font-size: 0.85rem;
      color: #94a3b8;
    }

    .action-success {
      margin-top: 20px;
    }
  `]
})
export class VerifyEmailComponent implements OnInit {
  email = '';
  codigo = '';
  loading = false;
  resendLoading = false;
  verified = false;
  hasPresetEmail = false;

  errorMessage = '';
  successMessage = '';
  infoMessage = '';

  constructor(private route: ActivatedRoute, private router: Router, private authService: AuthService) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['email']) {
        this.email = params['email'];
        this.hasPresetEmail = true;
      }
    });
  }

  onVerify(): void {
    if (!this.email || !this.codigo || this.codigo.length !== 6) return;

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.infoMessage = '';

    this.authService.verifyEmail(this.email, this.codigo).subscribe({
      next: (res) => {
        this.loading = false;
        this.verified = true;
        this.successMessage = res.mensaje || '¡Código verificado con éxito! Redirigiendo...';
        if (res.token || this.authService.isLoggedIn()) {
          setTimeout(() => {
            this.router.navigate(['/users']);
          }, 1500);
        }
      },
      error: (err) => {
        this.loading = false;
        if (err.error && err.error.message) {
          this.errorMessage = err.error.message;
        } else {
          this.errorMessage = 'Código incorrecto o ha ocurrido un error al verificar.';
        }
      }
    });
  }

  onResend(): void {
    if (!this.email) {
      this.errorMessage = 'Por favor, introduce tu correo electrónico primero.';
      return;
    }

    this.resendLoading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.infoMessage = '';

    this.authService.resendCode(this.email).subscribe({
      next: (res) => {
        this.resendLoading = false;
        this.infoMessage = res.mensaje || 'Se ha enviado un nuevo código de verificación.';
      },
      error: (err) => {
        this.resendLoading = false;
        if (err.error && err.error.message) {
          this.errorMessage = err.error.message;
        } else {
          this.errorMessage = 'No se pudo reenviar el código. Verifica el correo e intenta de nuevo.';
        }
      }
    });
  }
}
