import { Component, OnInit, computed, inject, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { HttpErrorResponse } from "@angular/common/http";
import { httpErrorMessage } from "../../core/utils/http-error";
import { forkJoin } from "rxjs";
import { AuthService } from "../../core/services/auth.service";
import { InventoryService } from "../../core/services/inventory.service";
import { ConfirmationDialogComponent } from "../../shared/components/confirmation-dialog/confirmation-dialog.component";
import {
  Product,
  ProductCommand,
  Category,
  Movement,
  MovementCommand,
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
  readonly query = signal("");
  readonly onlyLow = signal(false);
  page = 0;
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
  ngOnInit() {
    this.load();
  }
  readonly filtered = computed(() => {
    const q = this.query().trim().toLowerCase();
    return this.products().filter((p) =>
      (p.name + " " + p.sku + " " + p.categoryName).toLowerCase().includes(q) &&
      (!this.onlyLow() || (p.active && p.stock <= p.minStock)));
  });
  readonly activeCount = computed(() => this.products().filter(p => p.active).length);
  readonly lowCount = computed(() => this.products().filter(p => p.active && p.stock <= p.minStock).length);
  readonly units = computed(() => this.products().filter(p => p.active).reduce((n, p) => n + p.stock, 0));
  readonly value = computed(() => this.products().filter(p => p.active).reduce((n, p) => n + p.stock * p.price, 0));
  load() {
    this.loading = true;
    forkJoin({
      products: this.api.products(),
      categories: this.api.categories(),
      movements: this.api.movements(this.page),
    }).subscribe({
      next: (r) => {
        this.products.set(r.products);
        this.categories = r.categories;
        this.movements = r.movements;
        this.loading = false;
      },
      error: (e) => {
        this.loading = false;
        this.fail(e);
      },
    });
  }
  fail(e: HttpErrorResponse) {
    this.saving = false;
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
