package com.sakurastore.backend.application.usecase;
import com.sakurastore.backend.application.dto.PageResponse;
import com.sakurastore.backend.domain.exception.DomainException;
import java.time.LocalDate;
import java.util.function.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class ReportCsvService {
  private final ReportService reports;
  public ReportCsvService(ReportService reports) { this.reports = reports; }
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public String export(String report, LocalDate from, LocalDate to, Long categoryId, Long productId, String type) {
    return switch (report) {
      case "sales" -> csv("ID;Fecha;Cliente;Vendedor;Pago;Subtotal Bs;Descuento Bs;Total Bs", page -> reports.sales(from, to, page, 100),
        s -> new Object[] {s.id(), s.createdAt(), s.customerName(), s.sellerName(), s.paymentMethod(), s.subtotal(), s.discount(), s.total()});
      case "top-products" -> csv("Producto ID;SKU;Producto;Unidades;Importe bruto Bs", page -> reports.topProducts(from, to, page, 100),
        p -> new Object[] {p.productId(), p.sku(), p.name(), p.units(), p.grossAmount()});
      case "stock", "low-stock" -> csv("Producto ID;SKU;Producto;Categoría;Unidades;Mínimo;Precio Bs;Valor de venta Bs;Bajo stock",
        page -> reports.stock(categoryId, report.equals("low-stock"), page, 100), p -> new Object[] {p.productId(), p.sku(), p.name(), p.category(),
          p.quantity(), p.minimum(), p.price(), p.retailValue(), p.lowStock() ? "Sí" : "No"});
      case "movements" -> csv("ID;Fecha;Producto;Tipo;Cantidad;Stock anterior;Stock nuevo;Motivo;Responsable",
        page -> reports.movements(from, to, productId, type, page, 100), m -> new Object[] {m.id(), m.createdAt(), m.productName(), m.type(),
          m.quantity(), m.previousStock(), m.newStock(), m.reason(), m.actorName()});
      default -> throw new DomainException("Reporte no disponible.");
    };
  }
  private <T> String csv(String header, IntFunction<PageResponse<T>> fetch, Function<T, Object[]> fields) {
    var out = new StringBuilder("\uFEFF").append(header).append("\r\n"); int index = 0; PageResponse<T> page;
    do {
      page = fetch.apply(index++);
      if (page.totalElements() > 10000) throw new DomainException("La exportación admite hasta 10000 filas. Reduce el rango o aplica filtros.");
      for (T row : page.content()) {
        var values = fields.apply(row);
        for (int i = 0; i < values.length; i++) { if (i > 0) out.append(';'); out.append(cell(values[i])); }
        out.append("\r\n");
      }
    } while (!page.last());
    return out.toString();
  }
  static String cell(Object value) {
    String text = value == null ? "" : value.toString();
    if (value instanceof String && !text.isEmpty()) {
      String first = text.stripLeading();
      if (text.charAt(0) == '\t' || text.charAt(0) == '\r' || text.charAt(0) == '\n'
          || (!first.isEmpty() && "=+-@".indexOf(first.charAt(0)) >= 0)) text = "'" + text;
    }
    return "\"" + text.replace("\"", "\"\"") + "\"";
  }
}
