import { Product } from "./inventory.model";

export type SaleStatus = "COMPLETED" | "CANCELLED";


export interface SaleItemCommand {
    productId: number;
    quantity: number;
}

export interface SaleCommand {
    customerName?: string;
    discount: number;
    items: SaleItemCommand[];
}


export interface SaleItem {
    productId: number;
    productName: string;
    sku: string;
    quantity: number;
    unitPrice: number;
    subtotal: number;
}

export interface Sale {
    id: number;
    createdAt: string;
    customerName: string | null;
    sellerName: string;
    status: SaleStatus;
    subtotal: number;
    discount: number;
    total: number;
    items: SaleItem[];
}


export interface CartLine {
    product: Product;
    quantity: number;
}