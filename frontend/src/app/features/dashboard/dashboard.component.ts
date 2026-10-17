import { Component, OnDestroy, OnInit, computed, inject, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { HttpErrorResponse } from "@angular/common/http";
import { InventoryService } from "../../core/services/inventory.service";
import { DashboardSlide } from "../../core/models/dashboard.model";
import { ProductFilters } from "../../core/models/inventory.model";
import { httpErrorMessage } from "../../core/utils/http-error";
import { lowStockSlide } from "./dashboard-slides";

@Component({
    selector: "app-dashboard",
    standalone: true,
    imports: [CommonModule],
    templateUrl: "./dashboard.component.html",
    styleUrls: ["./dashboard.component.css"],
})
export class DashboardComponent implements OnInit, OnDestroy {
    private readonly api = inject(InventoryService);

    readonly slides = signal<DashboardSlide[]>([]);
    readonly loading = signal(true);
    readonly error = signal("");
    readonly current = signal(0);
    readonly hasSlides = computed(() => this.slides().length > 0);
    readonly currentSlide = computed(() => this.slides()[this.current()] ?? null);
    private timer?: ReturnType<typeof setInterval>;

    ngOnInit(): void {
        this.loadLowStock();
    }
    next(): void {
        const total = this.slides().length;
        if (total) this.current.set((this.current() + 1) % total);
    }

    prev(): void {
        const total = this.slides().length;
        if (total) this.current.set((this.current() - 1 + total) % total);
    }

    goTo(index: number): void {
        this.current.set(index);
        this.startAutoPlay(); // reinicia la cuenta para no saltar justo después del clic
    }

    pause(): void {
        this.stopAutoPlay();
    }

    resume(): void {
        this.startAutoPlay();
    }

    private startAutoPlay(): void {
        this.stopAutoPlay();
        if (this.slides().length > 1) {
            this.timer = setInterval(() => this.next(), 5000);
        }
    }

    private stopAutoPlay(): void {
        if (this.timer) clearInterval(this.timer);
        this.timer = undefined;
    }

    ngOnDestroy(): void {
        this.stopAutoPlay();
    }

    private loadLowStock(): void {
        const filters: ProductFilters = {
            name: "", sku: "", categoryId: 0, status: "", lowStock: true,
        };
        this.api.products(filters, 0, 12).subscribe({
            next: (page) => {
                this.slides.set(page.content.map(lowStockSlide));
                this.current.set(0);
                this.loading.set(false);
                this.startAutoPlay();
            },
            error: (e: HttpErrorResponse) => {
                this.error.set(httpErrorMessage(e, "No se pudo cargar el stock bajo."));
                this.loading.set(false);
            },
        });
    }
}