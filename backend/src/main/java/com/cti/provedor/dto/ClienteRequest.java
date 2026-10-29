package com.cti.provedor.dto;

import com.cti.provedor.model.NivelCliente;
import com.cti.provedor.model.StatusCliente;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ClienteRequest(
    @NotBlank @Size(max = 80) String codigoCti,
    @NotNull @Positive Long consultorId,
    @NotBlank @Size(max = 180) String nomeEmpresa,
    @NotBlank @Size(max = 100) String segmento,
    @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal faturamentoAnual,
    @NotNull NivelCliente nivel,
    StatusCliente status,
    @Email @Size(max = 254) String email) {}
