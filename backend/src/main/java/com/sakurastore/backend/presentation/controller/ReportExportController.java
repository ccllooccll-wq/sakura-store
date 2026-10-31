package com.sakurastore.backend.presentation.controller;
import com.sakurastore.backend.application.usecase.ReportCsvService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
public class ReportExportController {
  private final ReportCsvService csv;
  public ReportExportController(ReportCsvService csv) { this.csv = csv; }
  @GetMapping("/{report}/csv")
  public ResponseEntity<byte[]> export(@PathVariable String report, @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to, @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) Long productId, @RequestParam(required = false) String type) {
    byte[] body = csv.export(report, from, to, categoryId, productId, type).getBytes(StandardCharsets.UTF_8);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
      .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("sakura-" + report + ".csv").build().toString())
      .cacheControl(CacheControl.noStore()).body(body);
  }
}
