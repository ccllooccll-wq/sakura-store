import { Component, inject } from "@angular/core";
import { ReactiveFormsModule, NonNullableFormBuilder, Validators } from "@angular/forms";
import { Router, RouterLink } from "@angular/router";
import { HttpErrorResponse } from "@angular/common/http";
import { AuthService } from "../../../core/services/auth.service";
import { httpErrorMessage } from "../../../core/utils/http-error";
import { passwordMatch } from "../../../core/validators/password-match.validator";

@Component({
  selector: "app-register", standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: "./register.component.html", styleUrls: ["./register.component.css"],
})
export class RegisterComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  readonly form = this.fb.group({
    nombre: ["", [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)]],
    username: ["", [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
    email: ["", [Validators.required, Validators.email, Validators.maxLength(100)]],
    password: ["", [Validators.required, Validators.minLength(12), Validators.maxLength(72)]],
    confirmPassword: ["", Validators.required],
  }, { validators: passwordMatch("password", "confirmPassword") });
  loading = false;
  errorMessage = "";

  onSubmit(): void {
    if (this.loading) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const { confirmPassword, ...values } = this.form.getRawValue();
    const data = { ...values, nombre: values.nombre.trim(), username: values.username.trim(), email: values.email.trim() };
    this.loading = true;
    this.errorMessage = "";
    this.authService.register(data).subscribe({
      next: () => {
        this.loading = false;
        void this.router.navigate(["/verificar-correo"], { queryParams: { email: data.email } });
      },
      error: (error: HttpErrorResponse) => {
        this.loading = false;
        this.errorMessage = httpErrorMessage(error, "No se pudo completar el registro. Inténtalo nuevamente.");
      },
    });
  }
}
