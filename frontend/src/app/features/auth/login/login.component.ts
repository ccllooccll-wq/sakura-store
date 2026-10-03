import { Component, inject } from "@angular/core";

import { FormsModule } from "@angular/forms";
import { ActivatedRoute, Router, RouterLink } from "@angular/router";
import { AuthService } from "../../../core/services/auth.service";
import { HttpErrorResponse } from "@angular/common/http";
import { httpErrorMessage } from "../../../core/utils/http-error";

@Component({
  selector: "app-login",
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: "./login.component.html",
  styleUrls: ["./login.component.css"],
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  readonly sessionExpired = inject(ActivatedRoute).snapshot.queryParamMap.get("expired") === "1";
  username = "";
  password = "";
  loading = false;
  errorMessage = "";

  onSubmit() {
    if (this.loading || !this.username || !this.password) return;

    this.loading = true;
    this.errorMessage = "";

    this.authService.login(this.username, this.password).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.requiresOtp && res.email) {
          this.router.navigate(["/verificar-correo"], {
            queryParams: { email: res.email },
          });
        } else {
          this.router.navigate(["/inventario"]);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.errorMessage = httpErrorMessage(err, "Credenciales incorrectas o error de servidor.", "Usuario o contraseña incorrectos.");
      },
    });
  }
}
