package com.cti.provedor.dto;

import com.cti.provedor.model.Perfil;
import com.cti.provedor.model.StatusCadastro;
import com.cti.provedor.model.Usuario;
import java.time.OffsetDateTime;

public record CadastroAdminResponse(
    Long id,
    String nome,
    String email,
    Perfil perfil,
    StatusCadastro statusCadastro,
    OffsetDateTime cadastradoEm,
    OffsetDateTime revisadoEm,
    String revisadoPor) {
  public static CadastroAdminResponse from(Usuario u) {
    return new CadastroAdminResponse(
        u.getId(),
        u.getNome(),
        u.getEmail(),
        u.getPerfil(),
        u.getStatusCadastro(),
        u.getCadastradoEm(),
        u.getRevisadoEm(),
        u.getRevisadoPor());
  }
}
