package com.sakurastore.backend.infrastructure.persistence.repository;

import com.sakurastore.backend.domain.exception.DomainException;
import java.util.*;
import org.springframework.data.domain.*;

public final class QuerySupport {
  private QuerySupport() {}

  public static String contains(String value) {
    return "%"
        + value
            .trim()
            .toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        + "%";
  }

  public static boolean supplied(String value) {
    return value != null && !value.isBlank();
  }

  public static Pageable page(int page, int size, String sort, Map<String, String> fields) {
    if (page < 0 || page > 100000 || size < 1 || size > 100)
      throw new DomainException("Página inválida. El tamaño debe estar entre 1 y 100.");
    String[] parts = (sort == null ? "id,desc" : sort).split(",", -1);
    String field = fields.get(parts[0]);
    if (field == null || parts.length > 2)
      throw new DomainException("Columna de ordenación inválida.");
    String direction = parts.length == 1 ? "asc" : parts[1];
    if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc"))
      throw new DomainException("Usa asc o desc para ordenar.");
    Sort ordering = Sort.by(Sort.Direction.fromString(direction), field);
    if (!field.equals("id")) ordering = ordering.and(Sort.by("id"));
    return PageRequest.of(page, size, ordering);
  }
}
