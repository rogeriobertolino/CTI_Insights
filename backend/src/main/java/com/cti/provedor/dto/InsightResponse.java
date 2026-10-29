package com.cti.provedor.dto;

import com.cti.provedor.model.Insight;
import java.time.LocalDateTime;

public record InsightResponse(
    Long id,
    Long clienteId,
    Long contratoId,
    String tipo,
    String descricao,
    LocalDateTime dataGeracao) {
  public static InsightResponse from(Insight i) {
    return new InsightResponse(
        i.getId(),
        i.getCliente() == null ? null : i.getCliente().getId(),
        i.getContrato() == null ? null : i.getContrato().getId(),
        i.getTipo(),
        i.getDescricao(),
        i.getDataGeracao());
  }
}
