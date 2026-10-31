import { Product } from "./inventory.model";
import { SaleItem } from "./sales.model";

export interface PurchaseCommand {
  supplierReference?: string;
  items: { productId: number; quantity: number; unitPrice: number }[];
}
export interface Purchase {
  id: number;
  createdAt: string;
  total: number;
  actorId: number;
  actorName: string;
  supplierReference: string | null;
  items: SaleItem[];
}
export interface PurchaseLine {
  product: Product;
  quantity: number;
  unitPrice: number;
}
