import { DashboardSlide } from "../../core/models/dashboard.model";
import { Product } from "../../core/models/inventory.model";

export function lowStockSlide(p: Product): DashboardSlide {
    return {
        id: `low-${p.id}`,
        title: p.name,
        subtitle: `${p.sku} · ${p.categoryName}`,
        value: `${p.stock} / mín. ${p.minStock}`,
        tone: "warning",
    };
}