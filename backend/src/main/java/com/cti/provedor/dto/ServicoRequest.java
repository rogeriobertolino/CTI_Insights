package com.cti.provedor.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ServicoRequest(
    @NotBlank @Size(max = 150) String nome,
    @Size(max = 4000) String descricao,
    @Size(max = 100) String categoria,
    @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal preco) {}
