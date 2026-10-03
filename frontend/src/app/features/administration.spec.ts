import { HttpErrorResponse } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { of, throwError } from "rxjs";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AuthService } from "../core/services/auth.service";
import { UserService } from "../core/services/user.service";
import { InventoryService } from "../core/services/inventory.service";
import { Product } from "../core/models/inventory.model";
import { UserListComponent } from "./users/user-list/user-list.component";
import { InventoryComponent } from "./inventory/inventory.component";

describe("Roles e inventario", () => {
  const roles = [
    { id: 7, name: "ROLE_ADMIN" }, { id: 23, name: "ROLE_VENDEDOR" }, { id: 41, name: "ROLE_PROVEEDOR" },
  ];
  const user = { id: 2, username: "demo", fullName: "Demo", email: "demo@example.com", roleId: 23, roleName: "ROLE_VENDEDOR", active: true };
  const product: Product = { id: 1, sku: "SAK-001", name: "Demo", categoryId: 2, categoryName: "Accesorios", price: 10, stock: 50, minStock: 30, active: true };
  let admin: boolean;
  let users: { getUsers: ReturnType<typeof vi.fn>; getRoles: ReturnType<typeof vi.fn>; createUser: ReturnType<typeof vi.fn>; deleteUser: ReturnType<typeof vi.fn> };
  let inventory: { products: ReturnType<typeof vi.fn>; categories: ReturnType<typeof vi.fn>; movements: ReturnType<typeof vi.fn>; move: ReturnType<typeof vi.fn>; save: ReturnType<typeof vi.fn>; status: ReturnType<typeof vi.fn>; category: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    admin = true;
    // jsdom does not implement the browser dialog API.
    if (!HTMLDialogElement.prototype.showModal) HTMLDialogElement.prototype.showModal = function () { this.open = true; };
    users = { getUsers: vi.fn().mockReturnValue(of([user])), getRoles: vi.fn().mockReturnValue(of(roles)), createUser: vi.fn().mockReturnValue(of(user)), deleteUser: vi.fn().mockReturnValue(of({ mensaje: "Eliminado" })) };
    inventory = {
      products: vi.fn().mockReturnValue(of([product])), categories: vi.fn().mockReturnValue(of([{ id: 2, name: "Accesorios" }])), movements: vi.fn().mockReturnValue(of([])),
      move: vi.fn().mockReturnValue(of({})), save: vi.fn().mockReturnValue(of(product)), status: vi.fn().mockReturnValue(of(product)), category: vi.fn().mockReturnValue(of({})),
    };
    TestBed.configureTestingModule({ imports: [UserListComponent, InventoryComponent], providers: [
      { provide: AuthService, useValue: { isAdmin: () => admin, canManageInventory: () => admin } },
      { provide: UserService, useValue: users }, { provide: InventoryService, useValue: inventory },
    ] });
  });

  it("usa los IDs reales de roles y mantiene el select numérico", async () => {
    const fixture = TestBed.createComponent(UserListComponent);
    fixture.detectChanges();
    fixture.componentInstance.openCreateModal();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.componentInstance.formData.roleId).toBe(23);
    const select: HTMLSelectElement = fixture.nativeElement.querySelector('select[name="roleId"]');
    const option = Array.from(select.options).find(item => item.textContent?.includes("PROVEEDOR"))!;
    select.value = option.value;
    select.dispatchEvent(new Event("change", { bubbles: true }));
    await fixture.whenStable();
    expect(fixture.componentInstance.formData.roleId).toBe(41);
    expect(typeof fixture.componentInstance.formData.roleId).toBe("number");
  });

  it("muestra un fallo de roles, bloquea guardar y permite reintentar", async () => {
    users.getRoles.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    const fixture = TestBed.createComponent(UserListComponent);
    fixture.detectChanges();
    fixture.componentInstance.openCreateModal();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain("No se pudieron cargar los roles");
    expect(fixture.nativeElement.textContent).toContain("Reintentar");
    fixture.componentInstance.saveUser();
    expect(users.createUser).not.toHaveBeenCalled();
    users.getRoles.mockReturnValue(of(roles));
    fixture.componentInstance.loadRoles();
    expect(fixture.componentInstance.roles()).toHaveLength(3);
  });

  it("impide enviar desde el formulario administrativo una contraseña corta", async () => {
    const fixture = TestBed.createComponent(UserListComponent);
    fixture.detectChanges();
    fixture.componentInstance.openCreateModal();
    Object.assign(fixture.componentInstance.formData, { username: "demo123", fullName: "Demo", email: "demo@example.com", password: "short" });
    fixture.detectChanges();
    await fixture.whenStable();
    const form: HTMLFormElement = fixture.nativeElement.querySelector("form");
    form.dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));
    expect(users.createUser).not.toHaveBeenCalled();
  });

  it("eliminar un usuario requiere confirmar y cancelar no llama al backend", () => {
    const fixture = TestBed.createComponent(UserListComponent);
    fixture.detectChanges();
    fixture.componentInstance.deleteUser(user);
    fixture.detectChanges();
    expect(users.deleteUser).not.toHaveBeenCalled();
    const cancel: HTMLButtonElement = fixture.nativeElement.querySelector(".confirmation .btn-secondary");
    cancel.click();
    expect(fixture.componentInstance.userToDelete).toBeNull();
    expect(users.deleteUser).not.toHaveBeenCalled();
    fixture.componentInstance.deleteUser(user);
    fixture.componentInstance.confirmDelete();
    expect(users.deleteUser).toHaveBeenCalledWith(user.id);
  });

  it("el selector de movimientos envía SALIDA y rechaza cantidades mayores al stock", async () => {
    const fixture = TestBed.createComponent(InventoryComponent);
    fixture.detectChanges();
    fixture.componentInstance.openMovement(product);
    fixture.detectChanges();
    await fixture.whenStable();
    const select: HTMLSelectElement = fixture.nativeElement.querySelector('select[name="type"]');
    select.value = "SALIDA";
    select.dispatchEvent(new Event("change", { bubbles: true }));
    await fixture.whenStable();
    expect(fixture.componentInstance.movement.type).toBe("SALIDA");
    const quantity: HTMLInputElement = fixture.nativeElement.querySelector('input[name="quantity"]');
    quantity.value = "51";
    quantity.dispatchEvent(new Event("input", { bubbles: true }));
    fixture.componentInstance.movement.reason = "Venta";
    fixture.detectChanges();
    await fixture.whenStable();
    const form: HTMLFormElement = fixture.nativeElement.querySelector("form");
    form.dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));
    expect(inventory.move).not.toHaveBeenCalled();
  });

  it("un proveedor no ve acciones administrativas ni puede ejecutarlas desde el componente", () => {
    admin = false;
    const fixture = TestBed.createComponent(InventoryComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain("+ Nuevo producto");
    expect(fixture.nativeElement.querySelector("td.actions")).toBeNull();
    fixture.componentInstance.saveProduct();
    fixture.componentInstance.saveMovement();
    fixture.componentInstance.toggle(product);
    expect(inventory.save).not.toHaveBeenCalled();
    expect(inventory.move).not.toHaveBeenCalled();
    expect(inventory.status).not.toHaveBeenCalled();
    expect(fixture.componentInstance.statusProduct).toBeNull();
  });

  it("el resumen excluye productos inactivos y se actualiza al cambiar el stock", () => {
    const fixture = TestBed.createComponent(InventoryComponent);
    fixture.componentInstance.products.set([product, { ...product, id: 2, active: false, stock: 1000 }]);
    expect(fixture.componentInstance.activeCount()).toBe(1);
    expect(fixture.componentInstance.units()).toBe(50);
    expect(fixture.componentInstance.value()).toBe(500);
    fixture.componentInstance.products.set([{ ...product, stock: 20 }]);
    expect(fixture.componentInstance.lowCount()).toBe(1);
    expect(fixture.componentInstance.value()).toBe(200);
  });
});
