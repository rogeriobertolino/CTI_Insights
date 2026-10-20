package br.com.cti.insights.contracts;

import br.com.cti.insights.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ContratoResponse(
    Long id,
    Long clienteId,
    String cliente,
    Long servicoId,
    String servico,
    LocalDate dataInicio,
    LocalDate dataFim,
    BigDecimal valor,
    StatusContrato status) {
  public static ContratoResponse from(Contrato c) {
    return new ContratoResponse(
        c.getId(),
        c.getCliente().getId(),
        c.getCliente().getNomeEmpresa(),
        c.getServico().getId(),
        c.getServico().getNome(),
        c.getDataInicio(),
        c.getDataFim(),
        c.getValor(),
        c.getStatus());
  }
}
