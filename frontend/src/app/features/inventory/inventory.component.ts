import { Component, DestroyRef, OnInit, inject, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { HttpErrorResponse } from "@angular/common/http";
import { httpErrorMessage } from "../../core/utils/http-error";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { EMPTY, Subject, catchError, debounceTime, forkJoin, switchMap, tap } from "rxjs";
import { AuthService } from "../../core/services/auth.service";
import { InventoryService } from "../../core/services/inventory.service";
import { ConfirmationDialogComponent } from "../../shared/components/confirmation-dialog/confirmation-dialog.component";
import {
  Product,
  ProductCommand,
  Category,
  Movement,
  MovementCommand,
  InventorySummary,   // nuevo
  ProductFilters,     // nuevo
} from "../../core/models/inventory.model";
@Component({
  selector: "app-inventory",
  standalone: true,
  imports: [CommonModule, FormsModule, ConfirmationDialogComponent],
  templateUrl: "./inventory.component.html",
  styleUrls: ["./inventory.component.css"],
})
export class InventoryComponent implements OnInit {
  readonly products = signal<Product[]>([]);
  categories: Category[] = [];
  movements: Movement[] = [];
  loading = true;
  saving = false;
  error = "";
  success = "";
  readonly summary = signal<InventorySummary>({ activeCount: 0, lowCount: 0, units: 0, value: 0 });
  filters: ProductFilters = { name: "", sku: "", categoryId: 0, status: "", lowstock: false };
  readonly pageSizes = [10, 20, 50];
  pageSize = 10;
  productPage = 0;
  totalPages = 0;
  totalProducts = 0;
  page = 0; // página del historial de movimientos
  private readonly reload$ = new Subject<void>();
  private readonly typing$ = new Subject<void>();
  tab: "products" | "movements" = "products";
  modal: "product" | "movement" | "category" | null = null;
  editingId?: number;
  categoryName = "";
  selected?: Product;
  statusProduct: Product | null = null;
  form: ProductCommand = {
    sku: "",
    name: "",
    categoryId: 0,
    price: 0,
    minStock: 30,
  };
  movement: MovementCommand = {
    productId: 0,
    type: "ENTRADA",
    quantity: 1,
    reason: "",
  };
  private readonly api = inject(InventoryService);
  readonly auth = inject(AuthService);
  constructor() {
    // Carga de productos: switchMap descarta respuestas viejas si el usuario sigue filtrando.
    this.reload$
        .pipe(
            tap(() => (this.loading = true)),
            switchMap(() =>
                this.api.products(this.filters, this.productPage, this.pageSize).pipe(
                    catchError((e) => {
                      this.fail(e);
                      return EMPTY;
                    }),
                ),
            ),
            takeUntilDestroyed(inject(DestroyRef)),
        )
        .subscribe((res) => {
          // Si la página quedó fuera de rango (por ejemplo, se filtró), volver a la última válida.
          if (!res.content.length && this.productPage > 0 && res.totalPages > 0) {
            this.productPage = res.totalPages - 1;
            this.reload$.next();
            return;
          }
          this.products.set(res.content);
          this.totalProducts = res.totalElements;
          this.totalPages = res.totalPages;
          this.loading = false;
        });

    // Los inputs de texto esperan 300 ms antes de consultar al servidor.
    this.typing$
        .pipe(debounceTime(300), takeUntilDestroyed(inject(DestroyRef)))
        .subscribe(() => this.applyFilters());
  }

  ngOnInit() {
    this.load();
  }

  onTyping() {
    this.typing$.next();
  }

  applyFilters() {
    this.productPage = 0;
    this.reload$.next();
  }

  clearFilters() {
    this.filters = { name: "", sku: "", categoryId: 0, status: "", lowstock: false };
    this.applyFilters();
  }

  hasFilters() {
    const f = this.filters;
    return !!(f.name.trim() || f.sku.trim() || f.categoryId || f.status || f.lowstock);
  }

  changeProductPage(delta: number) {
    const next = this.productPage + delta;
    if (this.loading || next < 0 || next >= this.totalPages) return;
    this.productPage = next;
    this.reload$.next();
  }

  changePageSize() {
    this.applyFilters();
  }
  load() {
    this.loading = true;
    this.reload$.next();
    forkJoin({
      categories: this.api.categories(),
      movements: this.api.movements(this.page),
      summary: this.api.summary(),
    }).subscribe({
      next: (r) => {
        this.categories = r.categories;
        this.movements = r.movements;
        this.summary.set(r.summary);
      },
      error: (e) => {
        this.loading = false;
        this.fail(e);
      },
    });
  }
  fail(e: HttpErrorResponse) {
    this.saving = false;
    this.loading = false;
    this.error = httpErrorMessage(e, "No se pudo completar la operación.");
  }
  openProduct(p?: Product) {
    if (!this.auth.canManageInventory() || this.saving) return;
    this.error = "";
    this.editingId = p?.id;
    this.form = p
      ? {
          sku: p.sku,
          name: p.name,
          categoryId: p.categoryId,
          price: p.price,
          minStock: p.minStock,
        }
      : {
          sku: "",
          name: "",
          categoryId: this.categories[0]?.id ?? 0,
          price: 0,
          minStock: 30,
        };
    this.modal = "product";
  }
  saveProduct(formValid = true) {
    if (!formValid || !this.auth.canManageInventory() || this.saving) return;
    if (!this.categories.some(c => c.id === this.form.categoryId)) {
      this.error = "Selecciona una categoría activa. Si no hay ninguna, crea una primero.";
      return;
    }
    this.saving = true;
    this.api
      .save(this.form, this.editingId)
      .subscribe({
        next: () => this.done("Producto guardado."),
        error: (e) => this.fail(e),
      });
  }
  openMovement(p: Product) {
    if (!this.auth.canManageInventory() || this.saving || !p.active) return;
    this.error = "";
    this.selected = p;
    this.movement = {
      productId: p.id,
      type: "ENTRADA",
      quantity: 1,
      reason: "",
    };
    this.modal = "movement";
  }
  saveMovement(formValid = true) {
    if (!formValid || !this.auth.canManageInventory() || this.saving) return;
    this.saving = true;
    this.api.move(this.movement).subscribe({
      next: () => {
        this.page = 0;
        this.done("Movimiento registrado y stock actualizado.");
      },
      error: (e) => this.fail(e),
    });
  }
  saveCategory(formValid = true) {
    if (!formValid || !this.auth.canManageInventory() || this.saving || !this.categoryName.trim()) return;
    this.saving = true;
    this.api.category(this.categoryName.trim()).subscribe({
      next: () => {
        this.categoryName = "";
        this.done("Categoría creada.");
      },
      error: (e) => this.fail(e),
    });
  }
  toggle(p: Product) {
    if (!this.auth.canManageInventory() || this.saving) return;
    this.statusProduct = p;
  }
  confirmStatus() {
    if (!this.statusProduct || this.saving || !this.auth.canManageInventory()) return;
    const p = this.statusProduct;
    this.statusProduct = null;
    this.saving = true;
    this.api
      .status(p)
      .subscribe({
        next: () => this.done("Estado actualizado."),
        error: (e) => this.fail(e),
      });
  }
  done(message: string) {
    this.saving = false;
    this.modal = null;
    this.error = "";
    this.success = message;
    this.load();
  }
  close() {
    if (!this.saving) {
      this.modal = null;
      this.error = "";
    }
  }
  changePage(delta: number) {
    if (this.loading) return;
    this.page = Math.max(0, this.page + delta);
    this.load();
  }
}
