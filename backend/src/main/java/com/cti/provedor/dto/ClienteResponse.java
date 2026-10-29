package com.cti.provedor.dto;

import com.cti.provedor.model.Cliente;
import com.cti.provedor.model.NivelCliente;
import com.cti.provedor.model.StatusCliente;
import java.math.BigDecimal;

public record ClienteResponse(
    Long id,
    String codigoCti,
    Long consultorId,
    String consultor,
    String nomeEmpresa,
    String segmento,
    BigDecimal faturamentoAnual,
    NivelCliente nivel,
    StatusCliente status,
    String email) {
  public static ClienteResponse from(Cliente cliente) {
    return new ClienteResponse(
        cliente.getId(),
        cliente.getCodigoCti(),
        cliente.getConsultor().getId(),
        cliente.getConsultor().getNome(),
        cliente.getNomeEmpresa(),
        cliente.getSegmento(),
        cliente.getFaturamentoAnual(),
        cliente.getNivel(),
        cliente.getStatus(),
        cliente.getEmail());
  }
}
