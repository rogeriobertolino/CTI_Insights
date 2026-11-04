package com.cti.provedor.dto;

import jakarta.validation.constraints.*;

public record CadastroRequest(
    @NotBlank @Size(min = 3, max = 120) String nome,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 8, max = 72) String senha,
    @Size(max = 30) String matricula,
    @Size(max = 60) String turma) {
  public CadastroRequest(String nome, String email, String senha) { this(nome, email, senha, null, null); }
  @Override
  public String toString() {
    return "CadastroRequest[senha=[PROTEGIDA]]";
  }
}
