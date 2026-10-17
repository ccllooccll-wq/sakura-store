import { Component, inject, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { RouterModule, Router } from "@angular/router";
import { AuthService } from "../../../core/services/auth.service";
import { toSignal } from "@angular/core/rxjs-interop";
import { HttpErrorResponse } from "@angular/common/http";
import { httpErrorMessage } from "../../../core/utils/http-error";

@Component({
  selector: "app-navbar",
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: "./navbar.component.html",
  styleUrls: ["./navbar.component.css"],
})
export class NavbarComponent {
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  readonly currentUser = toSignal(this.authService.currentUser$, { initialValue: null });
  readonly logoutError = signal("");
  readonly loggingOut = signal(false);

  logout(): void {
    if (this.loggingOut()) return;
    this.loggingOut.set(true);
    this.logoutError.set("");
    this.authService.logout().subscribe({
      next: () => {
        this.loggingOut.set(false);
        void this.router.navigate(["/login"]);
      },
      error: (error: HttpErrorResponse) => {
        this.loggingOut.set(false);
        if (error.status === 401) {
          this.authService.clearSession();
          void this.router.navigate(["/login"]);
        } else this.logoutError.set(httpErrorMessage(error, "No se pudo cerrar sesión. Inténtalo nuevamente."));
      },
    });
  }

  getRoleDisplayName(roleName: string): string {
    switch (roleName) {
      case "ROLE_ADMIN":
        return "ADMINISTRADOR";
      case "ROLE_VENDEDOR":
        return "VENDEDOR";
      case "ROLE_PROVEEDOR":
        return "PROVEEDOR";
      default:
        return roleName;
    }
  }

  getRoleClass(roleName: string): string {
    switch (roleName) {
      case "ROLE_ADMIN":
        return "role-admin";
      case "ROLE_VENDEDOR":
        return "role-vendedor";
      case "ROLE_PROVEEDOR":
        return "role-proveedor";
      default:
        return "";
    }
  }
}
