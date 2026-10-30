package com.sakurastore.backend.application.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last) {
  @com.fasterxml.jackson.annotation.JsonProperty("number")
  public int number() { return page; }

  public static <S, T> PageResponse<T> from(Page<S> result, Function<S, T> mapper) {
    return new PageResponse<>(
        result.getContent().stream().map(mapper).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages(),
        result.isFirst(),
        result.isLast());
  }

  public <R> PageResponse<R> map(Function<T, R> mapper) {
    return new PageResponse<>(
        content.stream().map(mapper).toList(), page, size, totalElements, totalPages, first, last);
  }
}
