package com.cti.provedor.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Uma linha da planilha, como o Vue envia. As anotações são conferidas linha a linha pelo
 * ClienteValidacaoService: uma linha ruim é rejeitada sem derrubar o lote inteiro.
 */
public record ClienteDTO(
    Integer linha,
    @NotBlank(message = "código do cliente vazio") @Size(max = 80, message = "código com mais de 80 caracteres")
        String codigoCliente,
    @NotBlank(message = "nome do cliente vazio") @Size(max = 180, message = "nome com mais de 180 caracteres")
        String nomeCliente,
    @NotBlank(message = "consultor vazio") @Size(max = 150, message = "consultor com mais de 150 caracteres")
        String consultor,
    @NotBlank(message = "segmento vazio") @Size(max = 100, message = "segmento com mais de 100 caracteres")
        String segmento,
    @NotBlank(message = "nível vazio") @Pattern(regexp = "(?i)\\s*[ABC]\\s*", message = "nível deve ser A, B ou C")
        String nivelCliente,
    @NotNull(message = "faturamento anual vazio") @PositiveOrZero(message = "faturamento anual negativo")
        @Digits(integer = 13, fraction = 2, message = "faturamento anual fora do formato")
        BigDecimal faturamentoAnual,
    @NotBlank(message = "nenhum serviço contratado") @Size(max = 500, message = "serviços com mais de 500 caracteres")
        String servicosContratados,
    @NotBlank(message = "data de contratação vazia")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "data de contratação deve estar no formato AAAA-MM-DD")
        String dataContratacao,
    @Size(max = 120, message = "cidade com mais de 120 caracteres") String cidade,
    @Pattern(regexp = "(?i)\\s*([A-Z]{2})?\\s*", message = "UF deve ter 2 letras") String uf) {}
