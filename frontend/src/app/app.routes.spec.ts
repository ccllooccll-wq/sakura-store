import { TestBed } from "@angular/core/testing";
import { Router, provideRouter } from "@angular/router";
import { RouterTestingHarness } from "@angular/router/testing";
import { BehaviorSubject, of } from "rxjs";
import { beforeEach, describe, expect, it } from "vitest";
import { ReportsService } from "./core/services/reports.service";
import { routes } from "./app.routes";
import { User } from "./core/models/user.model";
import { AuthService } from "./core/services/auth.service";
import { UserService } from "./core/services/user.service";
import { SalesService } from "./core/services/sales.service";
import { InventoryService } from "./core/services/inventory.service";
import { PurchasesService } from "./core/services/purchases.service";

describe("Rutas protegidas", () => {
  let session: BehaviorSubject<User | null>;
  const admin: User = { id: 1, username: "demo", fullName: "Demo", email: "demo@example.com", roleId: 1, roleName: "ROLE_ADMIN", active: true };
  beforeEach(() => {
    session = new BehaviorSubject<User | null>(admin);
    TestBed.configureTestingModule({ providers: [provideRouter(routes),
      { provide: AuthService, useValue: {
        currentUser$: session.asObservable(), loadSession: () => of(session.value), getStoredUser: () => session.value,
        isAdmin: () => session.value?.roleName === "ROLE_ADMIN",
        canManageInventory: () => session.value?.roleName === "ROLE_ADMIN",
      } },
      { provide: UserService, useValue: { getUsers: () => of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 }), getRoles: () => of([]) } },
      { provide: InventoryService, useValue: { products: () => of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 }), categories: () => of([]), movements: () => of([]), summary: () => of({ activeCount: 0, lowCount: 0, units: 0, value: 0 }) } },
      { provide: SalesService, useValue: { list: () => of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 }) } },
      { provide: ReportsService, useValue: {page:()=>of({content:[],totalElements:0,totalPages:0,page:0,size:20}),summary:()=>of({salesCount:0,total:0,subtotal:0,discount:0,averageTicket:0})} },
      { provide: PurchasesService, useValue: { list: () => of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 }) } },
    ] });
  });

  it("una sesión anónima debe iniciar sesión para entrar al inventario", async () => {
    session.next(null);
    await RouterTestingHarness.create("/inventario");
    expect(TestBed.inject(Router).url).toBe("/login");
  });

  it("un vendedor tiene acceso al inventario y no a la administración de usuarios", async () => {
    session.next({ ...admin, roleId: 3, roleName: "ROLE_VENDEDOR" });
    await RouterTestingHarness.create("/usuarios");
    expect(TestBed.inject(Router).url).toBe("/inventario");
  });

  it("un cliente abre sus compras al intentar entrar a usuarios o inventario", async () => {
    session.next({ ...admin, roleId: 3, roleName: "ROLE_CLIENTE" });
    const harness = await RouterTestingHarness.create("/usuarios");
    expect(TestBed.inject(Router).url).toBe("/ventas");
    expect(harness.routeNativeElement?.querySelector('a[href="/inventario"]')).toBeNull();
    await harness.navigateByUrl("/inventario");
    expect(TestBed.inject(Router).url).toBe("/ventas");
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
  it("el administrador puede abrir compras de mercadería", async () => {
    const h = await RouterTestingHarness.create("/compras");
    expect(TestBed.inject(Router).url).toBe("/compras");
    expect(h.routeNativeElement?.textContent).toContain("Reposición de mercadería");
  });
  it("clientes y vendedores no pueden abrir compras de mercadería", async () => {
    session.next({ ...admin, roleName: "ROLE_CLIENTE" });
    const h = await RouterTestingHarness.create("/compras");
    expect(TestBed.inject(Router).url).toBe("/ventas");
    session.next({ ...admin, roleName: "ROLE_VENDEDOR" });
    await h.navigateByUrl("/compras");
    expect(TestBed.inject(Router).url).toBe("/inventario");
  });
 it("protege reportes para clientes y permite vendedores",async()=>{
 session.next({...admin,roleName:"ROLE_CLIENTE"});const h=await RouterTestingHarness.create("/reportes");expect(TestBed.inject(Router).url).toBe("/ventas");
 session.next({...admin,roleName:"ROLE_VENDEDOR"});await h.navigateByUrl("/reportes");expect(TestBed.inject(Router).url).toBe("/reportes");expect(h.routeNativeElement?.textContent).toContain("Reportes del");
 });

});
