import { Component, DestroyRef, OnInit, computed, inject, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { HttpErrorResponse } from "@angular/common/http";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { EMPTY, Subject, catchError, debounceTime, switchMap, tap } from "rxjs";
import { InventoryService } from "../../core/services/inventory.service";
import { PurchasesService } from "../../core/services/purchases.service";
import { Product } from "../../core/models/inventory.model";
import { Purchase, PurchaseCommand, PurchaseLine } from "../../core/models/purchases.model";
import { httpErrorMessage } from "../../core/utils/http-error";

import { PaginationComponent } from "../../shared/components/pagination/pagination.component";

const round2 = (n: number) => Math.round(n * 100) / 100;
const MAX_TOTAL = 9999999999.99;

@Component({
  selector: "app-purchases", standalone: true, imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: "./purchases.component.html",
  styleUrls: ["../sales/sales.component.css", "./purchases.component.css"],
})
export class PurchasesComponent implements OnInit {
  activeTab: 'order' | 'history' = 'order';
  orderStep: 'products' | 'detail' = 'products';
  private readonly inventory = inject(InventoryService);
  private readonly purchases = inject(PurchasesService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly search$ = new Subject<void>();
  private readonly typing$ = new Subject<void>();
  private historyRequest = 0;
  searchTerm = "";
  supplierReference = "";
  error = "";
  saveError = "";
  historyError = "";
  readonly results = signal<Product[]>([]);
  readonly searching = signal(false);
  readonly saving = signal(false);
  readonly cart = signal<PurchaseLine[]>([]);
  private readonly inputErrors = signal<Set<string>>(new Set());
  readonly lastPurchase = signal<Purchase | null>(null);
  readonly history = signal<Purchase[]>([]);
  readonly historyLoading = signal(false);
  historyPage = 0;
  historyTotal = 0;
  historyTotalPages = 0;
  readonly historySize = 10;
  readonly total = computed(() => round2(this.cart().reduce((n, l) => n + l.quantity * l.unitPrice, 0)));
  readonly itemCount = computed(() => this.cart().reduce((n, l) => n + l.quantity, 0));
  readonly invalid = computed(() => this.inputErrors().size > 0 || !this.cart().length || this.cart().length > 100
    || this.cart().some(l => !Number.isInteger(l.quantity) || l.quantity < 1 || l.quantity > 1000000
      || !Number.isFinite(l.unitPrice) || l.unitPrice < 0 || l.unitPrice > MAX_TOTAL)
    || !Number.isFinite(this.total()) || this.total() > MAX_TOTAL);
  constructor() {
    this.search$.pipe(
      tap(() => this.searching.set(true)),
      switchMap(() => this.inventory.products({
        name: this.searchTerm.trim(), sku: "", categoryId: 0, status: "ACTIVE", lowStock: false,
      }, 0, 8).pipe(catchError((e: HttpErrorResponse) => {
        this.error = httpErrorMessage(e, "No se pudieron cargar los productos.");
        this.searching.set(false); return EMPTY;
      }))), takeUntilDestroyed(this.destroyRef),
    ).subscribe(page => {
      this.results.set(page.content); this.error = ""; this.searching.set(false);
    });
    this.typing$.pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.search$.next());
  }
  ngOnInit(): void { this.search$.next(); this.loadHistory(); }
  onTyping(): void { this.typing$.next(); }
  addProduct(product: Product): void {
    if (this.saving() || !product.active) return;
    const old = this.cart().find(l => l.product.id === product.id);
    if (old) this.setQuantity(old, old.quantity + 1);
    else if (this.cart().length < 100)
      this.cart.update(lines => [...lines, { product, quantity: 1, unitPrice: 0 }]);
  }
  setQuantity(line: PurchaseLine, value: number | null): void {
    if (this.saving()) return;
    const quantity = Number(value);
    const valid = Number.isInteger(quantity) && quantity >= 1 && quantity <= 1000000;
    this.markInput(line.product.id + ":quantity", !valid);
    if (!valid) return;
    this.cart.update(lines => lines.map(l => l.product.id === line.product.id ? { ...l, quantity } : l));
  }
  setCost(line: PurchaseLine, value: number | null): void {
    if (this.saving()) return;
    const cost = value === null ? Number.NaN : Number(value);
    const valid = Number.isFinite(cost) && cost >= 0 && cost <= MAX_TOTAL;
    this.markInput(line.product.id + ":cost", !valid);
    if (!valid) return;
    this.cart.update(lines => lines.map(l =>
      l.product.id === line.product.id ? { ...l, unitPrice: round2(cost) } : l));
  }
  removeLine(line: PurchaseLine): void {
    if (this.saving()) return;
    this.cart.update(lines => lines.filter(l => l.product.id !== line.product.id));
    this.markInput(line.product.id + ":quantity", false);
    this.markInput(line.product.id + ":cost", false);
  }
  private markInput(key: string, invalid: boolean): void {
    this.inputErrors.update(previous => {
      const next = new Set(previous);
      if (invalid) next.add(key); else next.delete(key);
      return next;
    });
    this.saveError = this.inputErrors().size
      ? "Revisa las cantidades y los costos: utiliza valores válidos y completa los campos." : "";
  }
  clearCart(): void {
    if (this.saving()) return;
    this.cart.set([]); this.inputErrors.set(new Set()); this.supplierReference = ""; this.saveError = "";
  }
  confirmPurchase(): void {
    if (this.saving() || this.invalid() || this.supplierReference.trim().length > 200) return;
    const command: PurchaseCommand = {
      supplierReference: this.supplierReference.trim() || undefined,
      items: this.cart().map(l => ({ productId: l.product.id, quantity: l.quantity, unitPrice: l.unitPrice })),
    };
    this.saving.set(true); this.saveError = "";
    this.purchases.register(command).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: purchase => {
        this.saving.set(false); this.lastPurchase.set(purchase); this.clearCart();
        this.historyPage = 0; this.loadHistory(); this.search$.next();
      },
      error: (e: HttpErrorResponse) => {
        this.saving.set(false); this.saveError = httpErrorMessage(e, "No se pudo registrar la compra.");
      },
    });
  }
  loadHistory(): void {
    const request = ++this.historyRequest;
    this.historyLoading.set(true);
    this.purchases.list(this.historyPage, this.historySize).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: page => {
        if (request !== this.historyRequest) return;
        this.history.set(page.content); this.historyTotal = page.totalElements;
        this.historyTotalPages = page.totalPages; this.historyError = ""; this.historyLoading.set(false);
      },
      error: (e: HttpErrorResponse) => {
        if (request !== this.historyRequest) return;
        this.historyError = httpErrorMessage(e, "No se pudo cargar el historial."); this.historyLoading.set(false);
      },
    });
  }
  changeHistoryPage(delta: number): void {
    const next = this.historyPage + delta;
    if (this.historyLoading() || next < 0 || next >= this.historyTotalPages) return;
    this.historyPage = next; this.loadHistory();
  }
  onHistoryPageChange(page: number): void {
    if (this.historyLoading() || page < 0 || page >= this.historyTotalPages) return;
    this.historyPage = page; this.loadHistory();
  }
}
