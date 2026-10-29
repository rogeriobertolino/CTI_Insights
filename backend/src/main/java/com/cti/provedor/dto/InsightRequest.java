package com.cti.provedor.dto;

import jakarta.validation.constraints.*;

public record InsightRequest(
    @Positive Long clienteId,
    @Positive Long contratoId,
    @NotBlank @Size(max = 100) String tipo,
    @NotBlank @Size(max = 4000) String descricao) {}
