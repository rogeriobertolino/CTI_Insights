package com.cti.provedor.dto;

import jakarta.validation.constraints.*;
import java.util.List;

/** Corpo de POST /api/uploads/validadas: as linhas que passaram na validação do navegador. */
public record UploadRequest(
    @NotBlank @Size(max = 255) String arquivoNome,
    @Min(1) @Max(10000) int recebidas,
    @Min(0) int invalidas,
    @Min(0) int comAviso,
    @NotEmpty(message = "Envie ao menos uma linha.") @Size(max = 10000) List<ClienteDTO> linhas) {}
