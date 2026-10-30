package com.sakurastore.backend.application.port;
import com.sakurastore.backend.application.dto.PageResponse;
import com.sakurastore.backend.application.dto.ReportDtos.*;
import java.time.LocalDateTime;
public interface ReportRepositoryPort {
  SalesSummary summary(LocalDateTime from, LocalDateTime until);
  PageResponse<SaleRow> sales(LocalDateTime from, LocalDateTime until, int page, int size);
  PageResponse<TopProduct> topProducts(LocalDateTime from, LocalDateTime until, int page, int size);
  PageResponse<StockRow> stock(Long categoryId, boolean lowStock, int page, int size);
  PageResponse<MovementRow> movements(LocalDateTime from, LocalDateTime until, Long productId, String type, int page, int size);
}
