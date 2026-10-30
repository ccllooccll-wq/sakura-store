import { TestBed } from "@angular/core/testing";
import { Router, provideRouter } from "@angular/router";
import { RouterTestingHarness } from "@angular/router/testing";
import { BehaviorSubject, of } from "rxjs";
import { beforeEach, describe, expect, it } from "vitest";
import { routes } from "./app.routes";
import { User } from "./core/models/user.model";
import { AuthService } from "./core/services/auth.service";
import { UserService } from "./core/services/user.service";
import { InventoryService } from "./core/services/inventory.service";

describe("Rutas protegidas", () => {
  let session: BehaviorSubject<User | null>;
  const admin: User = { id: 1, username: "demo", fullName: "Demo", email: "demo@example.com", roleId: 1, roleName: "ROLE_ADMIN", active: true };
  beforeEach(() => {
    session = new BehaviorSubject<User | null>(admin);
    TestBed.configureTestingModule({ providers: [provideRouter(routes),
      { provide: AuthService, useValue: {
        currentUser$: session.asObservable(), loadSession: () => of(session.value),
        isAdmin: () => session.value?.roleName === "ROLE_ADMIN",
        canManageInventory: () => session.value?.roleName === "ROLE_ADMIN",
      } },
      { provide: UserService, useValue: { getUsers: () => of([]), getRoles: () => of([]) } },
      { provide: InventoryService, useValue: { products: () => of([]), categories: () => of([]), movements: () => of([]) } },
    ] });
  });

  it("una sesión anónima debe iniciar sesión para entrar al inventario", async () => {
    session.next(null);
    await RouterTestingHarness.create("/inventario");
    expect(TestBed.inject(Router).url).toBe("/login");
  });

  it("un proveedor tiene acceso al inventario y no a la administración de usuarios", async () => {
    session.next({ ...admin, roleId: 3, roleName: "ROLE_PROVEEDOR" });
    await RouterTestingHarness.create("/usuarios");
    expect(TestBed.inject(Router).url).toBe("/inventario");
  });

  it("conserva las rutas antiguas y reutiliza un solo navbar entre páginas", async () => {
    const harness = await RouterTestingHarness.create("/inventory");
    expect(TestBed.inject(Router).url).toBe("/inventario");
    const navbar = harness.routeNativeElement?.querySelector("app-navbar");
    expect(navbar).not.toBeNull();
    await harness.navigateByUrl("/users");
    expect(TestBed.inject(Router).url).toBe("/usuarios");
    expect(harness.routeNativeElement?.querySelectorAll("app-navbar")).toHaveLength(1);
    expect(harness.routeNativeElement?.querySelector("app-navbar")).toBe(navbar);
  });

  it("conserva el correo al abrir un enlace de verificación antiguo", async () => {
    const harness = await RouterTestingHarness.create("/verificar-email?email=demo%40example.com");
    const url = TestBed.inject(Router).parseUrl(TestBed.inject(Router).url);
    expect(url.root.children["primary"].segments[0].path).toBe("verificar-correo");
    expect(url.queryParamMap.get("email")).toBe("demo@example.com");
    expect(harness.routeNativeElement?.textContent).toContain("demo@example.com");
  });
});
