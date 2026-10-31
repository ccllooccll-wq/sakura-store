import { Injectable, inject } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { ReportFilters, ReportKind, ReportPage, SalesSummary } from "../models/reports.model";
@Injectable({ providedIn: "root" })
export class ReportsService {
  private readonly http = inject(HttpClient);
  private params(kind: ReportKind, f: ReportFilters) {
    let p = new HttpParams();
    if (kind === "stock" || kind === "low-stock") {
      if (f.categoryId) p = p.set("categoryId", f.categoryId);
      if (kind === "low-stock") p = p.set("lowStock", true);
    } else {
      p = p.set("from", f.from).set("to", f.to);
      if (kind === "movements") {
        if (f.productId) p = p.set("productId", f.productId);
        if (f.type) p = p.set("type", f.type);
      }
    }
    return p;
  }
  page(kind: ReportKind, f: ReportFilters, page: number, size: number) {
    return this.http.get<ReportPage>(`/api/reports/${kind === "low-stock" ? "stock" : kind}`,
      { params: this.params(kind, f).set("page", page).set("size", size) });
  }
  summary(f: ReportFilters) {
    return this.http.get<SalesSummary>("/api/reports/summary", { params: { from: f.from, to: f.to } });
  }
  export(kind: ReportKind, f: ReportFilters) {
    return this.http.get(`/api/reports/${kind}/csv`, { params: this.params(kind, f), responseType: "blob" });
  }
}
