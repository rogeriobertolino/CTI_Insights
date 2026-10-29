package com.cti.provedor.dto;

import jakarta.validation.constraints.*;

public record ConsultorRequest(
    @NotBlank @Size(max = 150) String nome,
    @NotBlank @Email @Size(max = 150) String email,
    @Size(max = 30) String telefone,
    @Size(max = 50) String matricula) {}
