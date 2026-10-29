package com.cti.provedor.service;

import org.springframework.data.domain.*;

public final class Paginacao {
  private Paginacao() {}

  public static Pageable criar(int pagina, int tamanho) {
    if (pagina < 0 || tamanho < 1 || tamanho > 100)
      throw new IllegalArgumentException(
          "Página deve ser positiva e tamanho deve estar entre 1 e 100.");
    return PageRequest.of(pagina, tamanho, Sort.by("id").descending());
  }
}
