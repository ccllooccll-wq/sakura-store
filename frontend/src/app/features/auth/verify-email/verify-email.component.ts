import { Component, OnInit, DestroyRef, inject } from "@angular/core";

import { FormsModule } from "@angular/forms";
import { ActivatedRoute, Router, RouterLink } from "@angular/router";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { AuthService } from "../../../core/services/auth.service";
import { HttpErrorResponse } from "@angular/common/http";
import { httpErrorMessage } from "../../../core/utils/http-error";

@Component({
  selector: "app-verify-email",
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: "./verify-email.component.html",
  styleUrls: ["./verify-email.component.css"],
})
export class VerifyEmailComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  email = "";
  codigo = "";
  loading = false;
  resendLoading = false;
  verified = false;
  hasPresetEmail = false;

  errorMessage = "";
  successMessage = "";
  infoMessage = "";

  ngOnInit(): void {
    this.route.queryParams.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      if (params["email"]) {
        this.email = params["email"];
        this.hasPresetEmail = true;
      }
    });
  }

  onVerify(): void {
    if (this.loading || !this.email || !/^\d{6}$/.test(this.codigo)) return;

    this.loading = true;
    this.errorMessage = "";
    this.successMessage = "";
    this.infoMessage = "";

    this.authService.verifyEmail(this.email, this.codigo).subscribe({
      next: (res) => {
        this.loading = false;
        this.verified = true;
        this.successMessage =
          res.mensaje || "¡Código verificado con éxito! Redirigiendo...";
        if (res.user || this.authService.isLoggedIn()) {
          void this.router.navigate(["/inventario"]);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.errorMessage = httpErrorMessage(err, "Código incorrecto o ha ocurrido un error al verificar.", "Código incorrecto o vencido. Solicita uno nuevo.");
      },
    });
  }

  onResend(): void {
    if (this.resendLoading || this.loading || this.verified) return;
    if (!this.email) {
      this.errorMessage = "Por favor, introduce tu correo electrónico primero.";
      return;
    }

    this.resendLoading = true;
    this.errorMessage = "";
    this.successMessage = "";
    this.infoMessage = "";

    this.authService.resendCode(this.email).subscribe({
      next: (res) => {
        this.resendLoading = false;
        this.infoMessage =
          res.mensaje || "Se ha enviado un nuevo código de verificación.";
      },
      error: (err: HttpErrorResponse) => {
        this.resendLoading = false;
        this.errorMessage = httpErrorMessage(err, "No se pudo reenviar el código. Verifica el correo e intenta de nuevo.", "Reinicia el proceso de acceso para solicitar un código.");
      },
    });
  }
}
