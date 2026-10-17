import { Injectable, inject } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";
import { Page } from "../models/inventory.model";
import { Sale, SaleCommand, SaleStatus } from "../models/sales.model";

@Injectable({ providedIn: "root" })
export class SalesService {
    private readonly url = "/api/sales";
    private readonly http = inject(HttpClient);

    register(command: SaleCommand): Observable<Sale> {
        return this.http.post<Sale>(this.url, command);
    }

    list(page = 0, size = 10, status?: SaleStatus): Observable<Page<Sale>> {
        let params = new HttpParams().set("page", page).set("size", size);
        if (status) params = params.set("status", status);
        return this.http.get<Page<Sale>>(this.url, { params });
    }
}