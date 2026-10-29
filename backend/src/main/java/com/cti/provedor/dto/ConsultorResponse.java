package com.cti.provedor.dto;

import com.cti.provedor.model.Consultor;

public record ConsultorResponse(
    Long id, String nome, String email, String telefone, String matricula) {
  public static ConsultorResponse from(Consultor c) {
    return new ConsultorResponse(
        c.getId(), c.getNome(), c.getEmail(), c.getTelefone(), c.getMatricula());
  }
}
