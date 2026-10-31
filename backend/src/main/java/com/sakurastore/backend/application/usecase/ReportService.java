package com.sakurastore.backend.application.usecase;
import com.sakurastore.backend.application.dto.PageResponse;
import com.sakurastore.backend.application.dto.ReportDtos.*;
import com.sakurastore.backend.application.port.ReportRepositoryPort;
import com.sakurastore.backend.domain.exception.DomainException;
import java.time.*;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly = true)
public class ReportService {
  private final ReportRepositoryPort repository;
  public ReportService(ReportRepositoryPort repository) { this.repository = repository; }
  public record Dates(LocalDateTime from, LocalDateTime until) {}
  public Dates dates(LocalDate from, LocalDate to) {
    if (from == null) from = LocalDate.now().withDayOfMonth(1);
    if (to == null) to = LocalDate.now();
    if (from.isAfter(to)) throw new DomainException("La fecha inicial no puede superar a la final.");
    if (from.getYear() < 1900 || to.getYear() > 9998) throw new DomainException("Las fechas deben estar entre los años 1900 y 9998.");
    return new Dates(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
  }
  private void paging(int page, int size) {
    if (page < 0 || page > 100000 || size < 1 || size > 100) throw new DomainException("Página inválida. Usa un tamaño entre 1 y 100.");
  }
  private void identifier(Long id) { if (id != null && id <= 0) throw new DomainException("El identificador debe ser positivo."); }
  public SalesSummary summary(LocalDate from, LocalDate to) { var d = dates(from, to); return repository.summary(d.from(), d.until()); }
  public PageResponse<SaleRow> sales(LocalDate from, LocalDate to, int page, int size) {
    paging(page, size); var d = dates(from, to); return repository.sales(d.from(), d.until(), page, size);
  }
  public PageResponse<TopProduct> topProducts(LocalDate from, LocalDate to, int page, int size) {
    paging(page, size); var d = dates(from, to); return repository.topProducts(d.from(), d.until(), page, size);
  }
  public PageResponse<StockRow> stock(Long categoryId, boolean lowStock, int page, int size) {
    paging(page, size); identifier(categoryId); return repository.stock(categoryId, lowStock, page, size);
  }
  public PageResponse<MovementRow> movements(LocalDate from, LocalDate to, Long productId, String type, int page, int size) {
    paging(page, size); identifier(productId); var d = dates(from, to);
    if (type != null && !type.isBlank() && !Set.of("ENTRADA", "SALIDA").contains(type)) throw new DomainException("Tipo de movimiento inválido.");
    return repository.movements(d.from(), d.until(), productId, type, page, size);
  }
}
