package com.cti.provedor.dto;

import com.cti.provedor.model.StatusCadastro;
import jakarta.validation.constraints.NotNull;

public record RevisaoCadastroRequest(@NotNull StatusCadastro decisao) {}
