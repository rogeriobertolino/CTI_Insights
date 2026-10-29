package com.cti.provedor.dto;

import com.cti.provedor.model.StatusContrato;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ContratoRequest(
    @NotNull @Positive Long clienteId,
    @NotNull @Positive Long servicoId,
    @NotNull LocalDate dataInicio,
    LocalDate dataFim,
    @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal valor,
    @NotNull StatusContrato status) {}
