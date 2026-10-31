import { Injectable, inject } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Page } from "../models/inventory.model";
import { Purchase, PurchaseCommand } from "../models/purchases.model";

@Injectable({ providedIn: "root" })
export class PurchasesService {
  private readonly http = inject(HttpClient);
  private readonly url = "/api/purchases";
  register(command: PurchaseCommand) { return this.http.post<Purchase>(this.url, command); }
  list(page = 0, size = 10) {
    const params = new HttpParams().set("page", page).set("size", size);
    return this.http.get<Page<Purchase>>(this.url, { params });
  }
}
