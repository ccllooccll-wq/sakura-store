import { Component, inject } from "@angular/core";
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { RouterLink } from "@angular/router";
import { HttpErrorResponse } from "@angular/common/http";
import { AuthService } from "../../../core/services/auth.service";
import { httpErrorMessage } from "../../../core/utils/http-error";
import { passwordMatch } from "../../../core/validators/password-match.validator";

@Component({
  selector: "app-reset-password", standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: "./reset-password.component.html", styleUrls: ["./reset-password.component.css"],
})
export class ResetPasswordComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly authService = inject(AuthService);
  readonly requestForm = this.fb.group({ email: ["", [Validators.required, Validators.email, Validators.maxLength(100)]] });
  readonly resetForm = this.fb.group({
    codigo: ["", [Validators.required, Validators.pattern(/^\d{6}$/)]],
    nuevaPassword: ["", [Validators.required, Validators.minLength(12), Validators.maxLength(72)]],
    confirmPassword: ["", Validators.required],
  }, { validators: passwordMatch("nuevaPassword", "confirmPassword") });
  step = 1;
  email = "";
  loading = false;
  passwordResetSuccess = false;
  errorMessage = "";
  successMessage = "";
  infoMessage = "";

  onRequestCode(): void {
    this.requestForm.markAllAsTouched();
    if (this.loading || this.requestForm.invalid) return;
    const email = this.requestForm.getRawValue().email.trim();
    this.loading = true;
    this.errorMessage = "";
    this.infoMessage = "";
    this.authService.requestPasswordReset(email).subscribe({
      next: (response) => {
        this.loading = false;
        this.email = email;
        this.step = 2;
        this.resetForm.reset();
        this.infoMessage = response.mensaje;
      },
      error: (error: HttpErrorResponse) => {
        this.loading = false;
        this.errorMessage = httpErrorMessage(error, "No se pudo solicitar el código. Inténtalo nuevamente.");
      },
    });
  }

  onResetPassword(): void {
    this.resetForm.markAllAsTouched();
    if (this.loading || this.resetForm.invalid || !this.email) return;
    const { codigo, nuevaPassword } = this.resetForm.getRawValue();
    this.loading = true;
    this.errorMessage = "";
    this.infoMessage = "";
    this.authService.resetPassword(this.email, codigo, nuevaPassword).subscribe({
      next: (response) => {
        this.loading = false;
        this.passwordResetSuccess = true;
        this.resetForm.reset();
        this.successMessage = response.mensaje;
      },
      error: (error: HttpErrorResponse) => {
        this.loading = false;
        this.errorMessage = httpErrorMessage(error, "No se pudo actualizar la contraseña. Revisa el código de 6 dígitos.", "Código incorrecto o vencido. Solicita uno nuevo.");
      },
    });
  }

  changeEmail(): void {
    if (this.loading) return;
    this.step = 1;
    this.resetForm.reset();
    this.errorMessage = "";
    this.infoMessage = "";
  }
}
