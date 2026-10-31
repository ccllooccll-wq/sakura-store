import { Component, DestroyRef, OnInit, inject, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { HttpErrorResponse } from "@angular/common/http";
import { EMPTY, Subject, catchError, forkJoin, switchMap } from "rxjs";
import { ReportsService } from "../../core/services/reports.service";
import { InventoryService } from "../../core/services/inventory.service";
import { ReportColumn, ReportFilters, ReportKind, ReportPage, SalesSummary } from "../../core/models/reports.model";
import { Category } from "../../core/models/inventory.model";
import { httpErrorMessage } from "../../core/utils/http-error";
const localDate = (date: Date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
const HEADERS: Record<ReportKind, ReportColumn[]> = {
  sales: [{key:"id",label:"Venta"},{key:"createdAt",label:"Fecha",format:"date"},{key:"customerName",label:"Cliente"},{key:"sellerName",label:"Vendedor"},{key:"paymentMethod",label:"Pago"},{key:"subtotal",label:"Subtotal",format:"money"},{key:"discount",label:"Descuento",format:"money"},{key:"total",label:"Total",format:"money"}],
  "top-products": [{key:"sku",label:"SKU"},{key:"name",label:"Producto"},{key:"units",label:"Unidades vendidas"},{key:"grossAmount",label:"Importe bruto",format:"money"}],
  stock: [{key:"sku",label:"SKU"},{key:"name",label:"Producto"},{key:"category",label:"Categoría"},{key:"quantity",label:"Stock"},{key:"minimum",label:"Mínimo"},{key:"retailValue",label:"Valor de venta",format:"money"},{key:"lowStock",label:"Bajo stock",format:"boolean"}],
  "low-stock": [],
  movements: [{key:"createdAt",label:"Fecha",format:"date"},{key:"productName",label:"Producto"},{key:"type",label:"Tipo"},{key:"quantity",label:"Cantidad"},{key:"previousStock",label:"Anterior"},{key:"newStock",label:"Nuevo"},{key:"reason",label:"Motivo"},{key:"actorName",label:"Responsable"}],
};
HEADERS["low-stock"] = HEADERS.stock;
@Component({ selector: "app-reports", standalone: true, imports: [CommonModule, FormsModule],
  templateUrl: "./reports.component.html", styleUrls: ["./reports.component.css"] })
export class ReportsComponent implements OnInit {
  private readonly api = inject(ReportsService);
  private readonly inventory = inject(InventoryService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new Subject<boolean>();
  readonly tabs: { id: ReportKind; label: string }[] = [
    {id:"sales",label:"Ventas"},{id:"top-products",label:"Más vendidos"},
    {id:"stock",label:"Stock actual"},{id:"low-stock",label:"Bajo stock"},{id:"movements",label:"Movimientos"}];
  kind: ReportKind = "sales";
  filters: ReportFilters = { from: localDate(new Date(new Date().getFullYear(), new Date().getMonth(), 1)),
    to: localDate(new Date()), categoryId: null, productId: null, type: "" };
  categories: Category[] = [];
  readonly result = signal<ReportPage | null>(null);
  readonly summary = signal<SalesSummary | null>(null);
  readonly loading = signal(false);
  readonly exporting = signal(false);
  error = "";
  categoryError = "";
  page = 0;
  size = 20;
  applied: ReportFilters | null = null;
  constructor() {
    this.reload$.pipe(switchMap(valid => {
      if (!valid) return EMPTY;
      const filters = {...this.filters};
      return forkJoin({page: this.api.page(this.kind, filters, this.page, this.size), summary: this.api.summary(filters)}).pipe(
        catchError((error: HttpErrorResponse) => { this.loading.set(false); this.error = httpErrorMessage(error, "No se pudo cargar el reporte."); return EMPTY; }));
    }), takeUntilDestroyed(this.destroyRef)).subscribe(data => {
      this.result.set(data.page); this.summary.set(data.summary); this.loading.set(false);
    });
  }
  ngOnInit() { this.loadCategories(); this.load(); }
  loadCategories() {
    this.inventory.categories().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: categories => { this.categories = categories; this.categoryError = ""; },
      error: () => this.categoryError = "No se pudieron cargar las categorías.",
    });
  }
  get stockReport() { return this.kind === "stock" || this.kind === "low-stock"; }
  get columns() { return HEADERS[this.kind]; }
  get pendingChanges() { return !!this.applied && JSON.stringify(this.filters) !== JSON.stringify(this.applied); }
  select(kind: ReportKind) { this.kind = kind; this.page = 0; this.load(); }
  apply() { this.page = 0; this.load(); }
  load() {
    this.reload$.next(false); this.loading.set(false);
    this.error = ""; this.result.set(null); this.summary.set(null);
    if (!this.filters.from || !this.filters.to || this.filters.from > this.filters.to) { this.error = "Selecciona un rango de fechas válido."; return; }
    if (this.kind === "movements" && this.filters.productId !== null && (!Number.isInteger(this.filters.productId) || this.filters.productId <= 0)) {
      this.error = "El ID del producto debe ser un entero positivo."; return;
    }
    this.applied = {...this.filters}; this.loading.set(true); this.reload$.next(true);
  }
  changePage(delta: number) {
    const next = this.page + delta;
    if (this.loading() || this.pendingChanges || next < 0 || next >= (this.result()?.totalPages ?? 0)) return;
    this.page = next; this.load();
  }
  money(value: number): string { return `Bs ${value.toLocaleString("es-BO", {minimumFractionDigits:2, maximumFractionDigits:2})}`; }
  value(row: Record<string, string | number | boolean | null>, c: ReportColumn): string {
    const value = row[c.key];
    if (value === null || value === undefined || value === "") return "—";
    if (c.format === "money") return this.money(Number(value));
    if (c.format === "date") return new Date(String(value)).toLocaleString("es-BO");
    if (c.format === "boolean") return value ? "Sí" : "No";
    return String(value);
  }
  download() {
    if (this.exporting() || this.loading() || this.pendingChanges || !this.result() || !this.applied) return;
    const kind = this.kind; this.exporting.set(true);
    this.api.export(kind, {...this.applied}).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob); const link = document.createElement("a");
        link.href = url; link.download = `sakura-${kind}.csv`; link.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000); this.exporting.set(false);
      },
      error: async (e: HttpErrorResponse) => {
        this.exporting.set(false);
        if (e.error instanceof Blob) {
          try { const body = JSON.parse(await e.error.text()); this.error = body.message || body.mensaje || "No se pudo exportar."; }
          catch { this.error = "No se pudo exportar. Inténtalo nuevamente."; }
        } else this.error = httpErrorMessage(e, "No se pudo exportar.");
      },
    });
  }
}
