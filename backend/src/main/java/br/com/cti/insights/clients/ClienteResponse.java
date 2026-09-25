package br.com.cti.insights.clients;

import java.math.BigDecimal;

import br.com.cti.insights.domain.Cliente;
import br.com.cti.insights.domain.NivelCliente;
import br.com.cti.insights.domain.StatusCliente;

public record ClienteResponse(
    Long id,
    String codigoCti,
    Long consultorId,
    String consultor,
    String nomeEmpresa,
    String segmento,
    BigDecimal faturamentoAnual,
    NivelCliente nivel,
    StatusCliente status
) {
    public static ClienteResponse from(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getCodigoCti(), cliente.getConsultor().getId(),
            cliente.getConsultor().getNome(), cliente.getNomeEmpresa(), cliente.getSegmento(),
            cliente.getFaturamentoAnual(), cliente.getNivel(), cliente.getStatus());
    }
}
