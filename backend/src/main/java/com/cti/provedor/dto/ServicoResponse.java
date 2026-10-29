package com.cti.provedor.dto;

import com.cti.provedor.model.Servico;
import java.math.BigDecimal;

public record ServicoResponse(
    Long id, String nome, String descricao, String categoria, BigDecimal preco) {
  public static ServicoResponse from(Servico s) {
    return new ServicoResponse(
        s.getId(), s.getNome(), s.getDescricao(), s.getCategoria(), s.getPreco());
  }
}
