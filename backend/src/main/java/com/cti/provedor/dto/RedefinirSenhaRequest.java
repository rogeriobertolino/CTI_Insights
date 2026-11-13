package com.cti.provedor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequest(
    @NotBlank String token, @NotBlank @Size(min = 8, max = 72) String senha) {
  @Override
  public String toString() {
    return "RedefinirSenhaRequest[token=[PROTEGIDO], senha=[PROTEGIDA]]";
  }
}