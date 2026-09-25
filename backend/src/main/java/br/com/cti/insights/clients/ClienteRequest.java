package br.com.cti.insights.clients;

import java.math.BigDecimal;

import br.com.cti.insights.domain.NivelCliente;
import br.com.cti.insights.domain.StatusCliente;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClienteRequest(
    @NotBlank String codigoCti,
    @NotNull Long consultorId,
    @NotBlank String nomeEmpresa,
    @NotBlank String segmento,
    @NotNull @DecimalMin("0.00") BigDecimal faturamentoAnual,
    @NotNull NivelCliente nivel,
    StatusCliente status
) {}
