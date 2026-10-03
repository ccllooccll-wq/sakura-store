import { Routes } from "@angular/router";
import { authGuard, adminGuard } from "./core/guards/auth.guard";

export const routes: Routes = [
  { path: "", redirectTo: "login", pathMatch: "full" },
  { path: "login", loadComponent: () => import("./features/auth/login/login.component").then(m => m.LoginComponent) },
  { path: "registro", loadComponent: () => import("./features/auth/register/register.component").then(m => m.RegisterComponent) },
  { path: "verificar-correo", loadComponent: () => import("./features/auth/verify-email/verify-email.component").then(m => m.VerifyEmailComponent) },
  { path: "recuperar-contrasena", loadComponent: () => import("./features/auth/reset-password/reset-password.component").then(m => m.ResetPasswordComponent) },
  { path: "users", redirectTo: "usuarios", pathMatch: "full" },
  { path: "inventory", redirectTo: "inventario", pathMatch: "full" },
  { path: "verificar-email", redirectTo: "verificar-correo", pathMatch: "full" },
  { path: "recuperar-password", redirectTo: "recuperar-contrasena", pathMatch: "full" },
  {
    path: "",
    loadComponent: () => import("./shared/components/app-shell/app-shell.component").then(m => m.AppShellComponent),
    canActivate: [authGuard],
    children: [
      { path: "usuarios", loadComponent: () => import("./features/users/user-list/user-list.component").then(m => m.UserListComponent), canActivate: [adminGuard] },
      { path: "inventario", loadComponent: () => import("./features/inventory/inventory.component").then(m => m.InventoryComponent) },
    ],
  },
  { path: "**", redirectTo: "login" },
];
