package br.com.cti.insights.services;

import br.com.cti.insights.domain.Servico;
import java.math.BigDecimal;

public record ServicoResponse(
    Long id, String nome, String descricao, String categoria, BigDecimal preco) {
  public static ServicoResponse from(Servico s) {
    return new ServicoResponse(
        s.getId(), s.getNome(), s.getDescricao(), s.getCategoria(), s.getPreco());
  }
}
