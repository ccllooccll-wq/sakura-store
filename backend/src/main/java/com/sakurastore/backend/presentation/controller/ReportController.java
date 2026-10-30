package com.sakurastore.backend.presentation.controller;
import com.sakurastore.backend.application.dto.PageResponse;
import com.sakurastore.backend.application.dto.ReportDtos.*;
import com.sakurastore.backend.application.usecase.ReportService;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
public class ReportController {
  private final ReportService reports;
  public ReportController(ReportService reports) { this.reports = reports; }
  @GetMapping("/summary")
  public SalesSummary summary(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) { return reports.summary(from, to); }
  @GetMapping("/sales")
  public PageResponse<SaleRow> sales(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return reports.sales(from, to, page, size); }
  @GetMapping("/top-products")
  public PageResponse<TopProduct> top(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return reports.topProducts(from, to, page, size); }
  @GetMapping("/stock")
  public PageResponse<StockRow> stock(@RequestParam(required = false) Long categoryId, @RequestParam(defaultValue = "false") boolean lowStock,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return reports.stock(categoryId, lowStock, page, size); }
  @GetMapping("/movements")
  public PageResponse<MovementRow> movements(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
      @RequestParam(required = false) Long productId, @RequestParam(required = false) String type, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) { return reports.movements(from, to, productId, type, page, size); }
}
