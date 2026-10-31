import { DashboardSlide } from "../../core/models/dashboard.model";
import { Product } from "../../core/models/inventory.model";

export function getProductImage(categoryName?: string, productName?: string): string {
    const text = `${categoryName ?? ""} ${productName ?? ""}`.toLowerCase();
    if (text.includes("figura") || text.includes("coleccion") || text.includes("anime") || text.includes("nendoroid")) {
        return "assets/dashboard/sakura-figures.svg";
    }
    if (text.includes("accesorio") || text.includes("llavero") || text.includes("joya") || text.includes("colgante") || text.includes("taza")) {
        return "assets/dashboard/sakura-accessories.svg";
    }
    if (text.includes("papeler") || text.includes("cuaderno") || text.includes("libreta") || text.includes("lapiz") || text.includes("sticker")) {
        return "assets/dashboard/sakura-stationery.svg";
    }
    if (text.includes("ropa") || text.includes("cosplay") || text.includes("manga") || text.includes("peluche") || text.includes("otaku")) {
        return "assets/dashboard/sakura-cosplay.svg";
    }
    return "assets/dashboard/sakura-low-stock.svg";
}

export function lowStockSlide(p: Product): DashboardSlide {
    return {
        id: `low-${p.id}`,
        title: p.name,
        subtitle: `${p.sku} · ${p.categoryName || "Sin categoría"}`,
        value: `${p.stock} unidades (mínimo: ${p.minStock})`,
        tone: "warning",
        imageUrl: getProductImage(p.categoryName, p.name),
        category: p.categoryName,
        badge: "Stock Bajo",
    };
}

export function defaultFeaturedSlides(): DashboardSlide[] {
    return [
        {
            id: "featured-welcome",
            title: "Bienvenido a Sakura Store",
            subtitle: "Inventario saludable · Todo el catálogo se encuentra con existencias óptimas",
            value: "Todo en orden ✓",
            tone: "ok",
            imageUrl: "assets/dashboard/sakura-store-banner.svg",
            badge: "Inventario OK",
        },
        {
            id: "featured-figures",
            title: "Coleccionables & Figuras",
            subtitle: "Explora la sección de colecciones, nendoroids y figuras importadas de anime",
            value: "Novedades Sakura",
            tone: "info",
            imageUrl: "assets/dashboard/sakura-figures.svg",
            badge: "Tendencia",
        },
        {
            id: "featured-cosplay",
            title: "Manga, Cosplay & Merch Especial",
            subtitle: "Accesorios icónicos, varitas mágicas y artículos de colección seleccionados",
            value: "Colección Exclusiva",
            tone: "info",
            imageUrl: "assets/dashboard/sakura-cosplay.svg",
            badge: "Edición Limitada",
        },
        {
            id: "featured-promo",
            title: "Promociones & Envíos de Temporada",
            subtitle: "Empaques temáticos y descuentos especiales para clientes frecuentes",
            value: "Temporada Sakura",
            tone: "ok",
            imageUrl: "assets/dashboard/sakura-promo.svg",
            badge: "Beneficios",
        },
        {
            id: "featured-accessories",
            title: "Accesorios & Papelería",
            subtitle: "Joyería, libretas, stickers y artículos inspirados en los cerezos de Japón",
            value: "Catálogo Activo",
            tone: "ok",
            imageUrl: "assets/dashboard/sakura-accessories.svg",
            badge: "Destacado",
        },
    ];
}
