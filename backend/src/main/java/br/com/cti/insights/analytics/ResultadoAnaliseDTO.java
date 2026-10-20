package br.com.cti.insights.analytics;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;

public record ResultadoAnaliseDTO(
    @NotNull UUID importacaoId,
    @NotNull @Size(max = 20)
        Map<
                @NotBlank @Size(max = 40) String,
                @DecimalMin("0") @DecimalMax("10000000000000000000000000000") BigDecimal>
            estatisticas,
    @NotNull @Size(max = 30) List<@NotBlank @Size(max = 2000) String> insights) {}
