import { Routes } from "@angular/router";
import { authGuard, adminGuard, staffGuard } from "./core/guards/auth.guard";

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
      { path: "dashboard", loadComponent: () => import("./features/dashboard/dashboard.component").then(m => m.DashboardComponent), canActivate: [adminGuard] },
      { path: "usuarios", loadComponent: () => import("./features/users/user-list/user-list.component").then(m => m.UserListComponent), canActivate: [adminGuard] },
      { path: "inventario", loadComponent: () => import("./features/inventory/inventory.component").then(m => m.InventoryComponent), canActivate: [staffGuard] },
      { path: "ventas", loadComponent: () => import("./features/sales/sales.component").then(m => m.SalesComponent) },
      { path: "reportes", loadComponent: () => import("./features/reports/reports.component").then(m => m.ReportsComponent), canActivate: [staffGuard] },
      { path: "compras", loadComponent: () => import("./features/purchases/purchases.component").then(m => m.PurchasesComponent), canActivate: [adminGuard] },
    ],
  },
  { path: "**", redirectTo: "login" },
];
