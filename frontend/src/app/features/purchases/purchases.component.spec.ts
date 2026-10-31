import { TestBed } from "@angular/core/testing";
import { HttpErrorResponse } from "@angular/common/http";
import { Subject, of, throwError } from "rxjs";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { PurchasesComponent } from "./purchases.component";
import { PurchasesService } from "../../core/services/purchases.service";
import { InventoryService } from "../../core/services/inventory.service";
import { Product } from "../../core/models/inventory.model";
import { Purchase } from "../../core/models/purchases.model";

describe("Compras de mercadería", () => {
  const product: Product = { id: 1, sku: "COMPRA-1", name: "Cuaderno", categoryId: 1,
    categoryName: "Papelería", price: 12.5, stock: 0, minStock: 30, active: true };
  const saved: Purchase = { id: 15, createdAt: "2026-10-17T12:00:00", total: 12.5, actorId: 2,
    actorName: "Administrador", supplierReference: "Factura 123", items: [
      { productId: 1, productName: "Cuaderno", sku: "COMPRA-1", quantity: 2, unitPrice: 6.25, subtotal: 12.5 }] };
  let purchases: { register: ReturnType<typeof vi.fn>; list: ReturnType<typeof vi.fn> };
  let inventory: { products: ReturnType<typeof vi.fn>; move: ReturnType<typeof vi.fn> };
  beforeEach(() => {
    purchases = { register: vi.fn().mockReturnValue(of(saved)), list: vi.fn().mockReturnValue(of({
      content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 })) };
    inventory = { products: vi.fn().mockReturnValue(of({
      content: [product], totalElements: 1, totalPages: 1, number: 0, size: 8 })), move: vi.fn() };
    TestBed.configureTestingModule({ imports: [PurchasesComponent], providers: [
      { provide: PurchasesService, useValue: purchases }, { provide: InventoryService, useValue: inventory }] });
  });
  it("permite reponer un producto sin stock", () => {
    const f = TestBed.createComponent(PurchasesComponent); f.detectChanges();
    expect(f.nativeElement.textContent).toContain("Cuaderno");
    f.componentInstance.addProduct(product); expect(f.componentInstance.cart()).toHaveLength(1);
  });
  it("envía costos y cantidades sin duplicar el movimiento desde el navegador", () => {
    const f = TestBed.createComponent(PurchasesComponent); f.detectChanges(); const c = f.componentInstance;
    c.addProduct(product); c.addProduct(product); c.setCost(c.cart()[0], 6.25);
    c.supplierReference = " Factura 123 "; expect(c.total()).toBe(12.5); c.confirmPurchase();
    expect(purchases.register).toHaveBeenCalledWith({
      supplierReference: "Factura 123", items: [{ productId: 1, quantity: 2, unitPrice: 6.25 }] });
    expect(inventory.move).not.toHaveBeenCalled(); expect(c.cart()).toHaveLength(0);
    expect(c.lastPurchase()?.id).toBe(15); expect(purchases.list).toHaveBeenCalledTimes(2);
    expect(inventory.products).toHaveBeenCalledTimes(2);
  });
  it("impide enviar cantidades y costos inválidos", () => {
    const f = TestBed.createComponent(PurchasesComponent); f.detectChanges(); const c = f.componentInstance;
    c.addProduct(product); c.setQuantity(c.cart()[0], 0); c.confirmPurchase();
    expect(c.invalid()).toBe(true); expect(purchases.register).not.toHaveBeenCalled();
    c.setQuantity(c.cart()[0], 2); c.setCost(c.cart()[0], -1); c.confirmPurchase();
    expect(c.invalid()).toBe(true); expect(purchases.register).not.toHaveBeenCalled();
    c.setCost(c.cart()[0], null); expect(c.invalid()).toBe(true);
    expect(() => f.detectChanges()).not.toThrow();
  });
  it("impide doble envío mientras espera al servidor", () => {
    const pending = new Subject<Purchase>(); purchases.register.mockReturnValue(pending);
    const f = TestBed.createComponent(PurchasesComponent); f.detectChanges(); const c = f.componentInstance;
    c.addProduct(product); c.confirmPurchase(); c.confirmPurchase();
    expect(purchases.register).toHaveBeenCalledTimes(1); expect(c.saving()).toBe(true);
    pending.next(saved); pending.complete(); expect(c.saving()).toBe(false); expect(c.cart()).toHaveLength(0);
  });
  it("conserva el detalle y muestra el rechazo del servidor", () => {
    purchases.register.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 400, error: { message: "El producto está desactivado." } })));
    const f = TestBed.createComponent(PurchasesComponent); f.detectChanges(); const c = f.componentInstance;
    c.addProduct(product); c.confirmPurchase(); f.detectChanges();
    expect(c.cart()).toHaveLength(1); expect(c.saving()).toBe(false);
    expect(f.nativeElement.textContent).toContain("El producto está desactivado.");
  });
});
