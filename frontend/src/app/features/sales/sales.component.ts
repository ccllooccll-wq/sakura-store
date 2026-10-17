import { Component, DestroyRef, OnInit, computed, inject, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { EMPTY, Subject, catchError, debounceTime, switchMap, tap } from "rxjs";
import { HttpErrorResponse } from "@angular/common/http";
import { InventoryService } from "../../core/services/inventory.service";
import { Product } from "../../core/models/inventory.model";
import { httpErrorMessage } from "../../core/utils/http-error";
import { SalesService } from "../../core/services/sales.service";
import { CartLine, Sale, SaleCommand } from "../../core/models/sales.model";

const round2 = (n: number) => Math.round(n * 100) / 100;

@Component({
    selector: "app-sales",
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: "./sales.component.html",
    styleUrls: ["./sales.component.css"],
})
export class SalesComponent implements OnInit {
    private readonly inventory = inject(InventoryService);
    private readonly sales = inject(SalesService);

    readonly saving = signal(false);
    readonly lastSale = signal<Sale | null>(null);
    saleError = "";


    readonly history = signal<Sale[]>([]);
    readonly historyLoading = signal(false);
    historyError = "";
    historyPage = 0;
    readonly historySize = 10;
    historyTotalPages = 0;
    historyTotal = 0;
    private historyRequest = 0;


    searchTerm = "";
    readonly results = signal<Product[]>([]);
    readonly searching = signal(false);
    error = "";
    private readonly search$ = new Subject<void>();
    private readonly typing$ = new Subject<void>();


    readonly cart = signal<CartLine[]>([]);
    readonly discount = signal(0);
    customerName = "";

    readonly itemCount = computed(() => this.cart().reduce((n, l) => n + l.quantity, 0));
    readonly subtotal = computed(() =>
        round2(this.cart().reduce((n, l) => n + l.product.price * l.quantity, 0)));
    readonly total = computed(() => round2(Math.max(0, this.subtotal() - this.discount())));
    readonly discountInvalid = computed(
        () => this.discount() < 0 || this.discount() > this.subtotal());

    constructor() {

        this.search$
            .pipe(
                tap(() => this.searching.set(true)),
                switchMap(() =>
                    this.inventory
                        .products(
                            { name: this.searchTerm.trim(), sku: "", categoryId: 0, status: "ACTIVE", lowstock: false },
                            0,
                            8,
                        )
                        .pipe(
                            catchError((e: HttpErrorResponse) => {
                                this.error = httpErrorMessage(e, "No se pudieron cargar los productos.");
                                this.searching.set(false);
                                return EMPTY;
                            }),
                        ),
                ),
                takeUntilDestroyed(inject(DestroyRef)),
            )
            .subscribe((page) => {
                this.error = "";
                this.results.set(page.content);
                this.searching.set(false);
            });


        this.typing$
            .pipe(debounceTime(300), takeUntilDestroyed(inject(DestroyRef)))
            .subscribe(() => this.search$.next());
    }

    ngOnInit(): void {
        this.search$.next();
        this.loadHistory();
    }

    onTyping(): void {
        this.typing$.next();
    }


    quantityInCart(p: Product): number {
        return this.cart().find((l) => l.product.id === p.id)?.quantity ?? 0;
    }

    canAdd(p: Product): boolean {
        return p.active && p.stock > this.quantityInCart(p);
    }

    addProduct(p: Product): void {
        if (!this.canAdd(p)) return;
        this.cart.update((lines) =>
            lines.some((l) => l.product.id === p.id)
                ? lines.map((l) => (l.product.id === p.id ? { ...l, quantity: l.quantity + 1 } : l))
                : [...lines, { product: p, quantity: 1 }],
        );
    }

    changeQuantity(line: CartLine, delta: number): void {
        this.applyQuantity(line, line.quantity + delta);
    }

    setQuantity(line: CartLine, input: HTMLInputElement): void {
        const typed = Math.floor(Number(input.value));
        if (!Number.isFinite(typed)) {
            input.value = String(line.quantity);
            return;
        }
        input.value = String(this.applyQuantity(line, typed));
    }


    private applyQuantity(line: CartLine, wanted: number): number {
        const quantity = Math.min(Math.max(wanted, 1), line.product.stock);
        this.cart.update((lines) =>
            lines.map((l) => (l.product.id === line.product.id ? { ...l, quantity } : l)));
        return quantity;
    }

    removeLine(line: CartLine): void {
        this.cart.update((lines) => lines.filter((l) => l.product.id !== line.product.id));
    }

    setDiscount(value: number | null): void {
        this.discount.set(round2(Number(value) || 0));
    }

    clearCart(): void {
        this.cart.set([]);
        this.discount.set(0);
        this.customerName = "";
    }
    confirmSale(): void {
        if (this.saving() || !this.cart().length || this.discountInvalid()) return;
        const command: SaleCommand = {
            customerName: this.customerName.trim() || undefined,
            discount: this.discount(),
            items: this.cart().map((l) => ({ productId: l.product.id, quantity: l.quantity })),
        };
        this.saving.set(true);
        this.saleError = "";
        this.sales.register(command).subscribe({
            next: (sale) => {
                this.saving.set(false);
                this.lastSale.set(sale);
                this.clearCart();
                this.search$.next();   // el stock cambió: refrescar los resultados
                this.historyPage = 0;
                this.loadHistory();
            },
            error: (e: HttpErrorResponse) => {
                this.saving.set(false);
                this.saleError = httpErrorMessage(e, "No se pudo registrar la venta.");
                this.search$.next();   // puede que el stock haya cambiado
            },
        });
    }

    dismissLastSale(): void {
        this.lastSale.set(null);
    }
    loadHistory(): void {
        const request = ++this.historyRequest;
        this.historyLoading.set(true);
        this.sales.list(this.historyPage, this.historySize).subscribe({
            next: (page) => {
                if (request !== this.historyRequest) return; // llegó una respuesta vieja
                this.history.set(page.content);
                this.historyTotal = page.totalElements;
                this.historyTotalPages = page.totalPages;
                this.historyError = "";
                this.historyLoading.set(false);
            },
            error: (e: HttpErrorResponse) => {
                if (request !== this.historyRequest) return;
                this.historyError = httpErrorMessage(e, "No se pudo cargar el historial de ventas.");
                this.historyLoading.set(false);
            },
        });
    }

    changeHistoryPage(delta: number): void {
        const next = this.historyPage + delta;
        if (this.historyLoading() || next < 0 || next >= this.historyTotalPages) return;
        this.historyPage = next;
        this.loadHistory();
    }
}