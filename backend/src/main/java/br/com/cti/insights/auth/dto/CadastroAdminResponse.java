package br.com.cti.insights.auth.dto;

import br.com.cti.insights.auth.*;
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
