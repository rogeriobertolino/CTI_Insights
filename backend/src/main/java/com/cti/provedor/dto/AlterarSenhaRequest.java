package com.cti.provedor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarSenhaRequest(
    @NotBlank String senhaAtual, @NotBlank @Size(min = 8, max = 72) String novaSenha) {
  @Override
  public String toString() {
    return "AlterarSenhaRequest[senhaAtual=[PROTEGIDA], novaSenha=[PROTEGIDA]]";
  }
}
