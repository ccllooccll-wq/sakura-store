import { Injectable, inject } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Category, Product, ProductCommand, Movement, MovementCommand, Page, ProductFilters, InventorySummary } from "../models/inventory.model";
@Injectable({ providedIn: "root" })
export class InventoryService {
  private readonly url = "/api/inventory";
  private readonly http = inject(HttpClient);
  products(f: ProductFilters, page = 0, size = 10) {
    let params = new HttpParams().set("page", page).set("size", size);
    if (f.name.trim()) params = params.set("name", f.name.trim());
    if (f.sku.trim()) params = params.set("sku", f.sku.trim());
    if (f.categoryId > 0) params = params.set("categoryId", f.categoryId);
    if (f.status) params = params.set("status", f.status);
    if (f.lowStock) params = params.set("lowStock", true);
    return this.http.get<Page<Product>>(`${this.url}/products`, { params });
  }
  summary() {
    return this.http.get<InventorySummary>(`${this.url}/summary`);
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
