package com.cti.provedor.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaResposta<T>(
    List<T> content, long totalElements, int number, int totalPages, int size) {
  public static <T> PaginaResposta<T> from(Page<T> page) {
    return new PaginaResposta<>(
        page.getContent(),
        page.getTotalElements(),
        page.getNumber(),
        page.getTotalPages(),
        page.getSize());
  }
}
