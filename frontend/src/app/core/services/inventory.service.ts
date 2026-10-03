import { Injectable, inject } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Category, Product, ProductCommand, Movement, MovementCommand } from "../models/inventory.model";
@Injectable({ providedIn: "root" })
export class InventoryService {
  private readonly url = "/api/inventory";
  private readonly http = inject(HttpClient);
  products() {
    return this.http.get<Product[]>(`${this.url}/products`);
  }
  categories() {
    return this.http.get<Category[]>(`${this.url}/categories`);
  }
  category(name: string) {
    return this.http.post<Category>(`${this.url}/categories`, { name });
  }
  save(c: ProductCommand, id?: number) {
    return id
      ? this.http.put<Product>(`${this.url}/products/${id}`, c)
      : this.http.post<Product>(`${this.url}/products`, c);
  }
  status(p: Product) {
    return this.http.patch<void>(`${this.url}/products/${p.id}/status`, {
      active: !p.active,
    });
  }
  movements(page = 0, productId?: number) {
    let params = new HttpParams().set("page", page);
    if (productId) params = params.set("productId", productId);
    return this.http.get<Movement[]>(`${this.url}/movements`, { params });
  }
  move(c: MovementCommand) {
    return this.http.post<Movement>(`${this.url}/movements`, c);
  }
}
