export type ReportKind = "sales" | "top-products" | "stock" | "low-stock" | "movements";
export interface ReportFilters { from: string; to: string; categoryId: number | null; productId: number | null; type: string; }
export interface SalesSummary { salesCount: number; subtotal: number; discount: number; total: number; averageTicket: number; }
export interface ReportPage { content: Record<string, string | number | boolean | null>[]; totalElements: number; totalPages: number; page: number; size: number; }
export interface ReportColumn { key: string; label: string; format?: "money" | "date" | "boolean"; }
