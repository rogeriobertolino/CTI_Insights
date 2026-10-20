package br.com.cti.insights.auth.dto;

import br.com.cti.insights.auth.StatusCadastro;
import jakarta.validation.constraints.NotNull;

public record RevisaoCadastroRequest(@NotNull StatusCadastro decisao) {}
