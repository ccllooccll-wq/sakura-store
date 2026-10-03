export interface Category {
  id: number;
  name: string;
}

export interface Product {
  id: number;
  sku: string;
  name: string;
  categoryId: number;
  categoryName: string;
  price: number;
  minStock: number;
  stock: number;
  active: boolean;
}

export type ProductCommand = Pick<Product,
  "sku" | "name" | "categoryId" | "price" | "minStock">;

export interface Movement {
  id: number;
  productId: number;
  productName: string;
  type: string;
  quantity: number;
  previousStock: number;
  newStock: number;
  reason: string;
  actorName: string;
  createdAt: string;
}

export interface MovementCommand {
  productId: number;
  type: "ENTRADA" | "SALIDA";
  quantity: number;
  reason: string;
}
