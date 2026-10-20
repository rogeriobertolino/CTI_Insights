package br.com.cti.insights.auth.dto;

import br.com.cti.insights.auth.*;

public record UsuarioResponse(
    Long id, String nome, String email, Perfil perfil, StatusCadastro statusCadastro) {
  public static UsuarioResponse from(Usuario u) {
    return new UsuarioResponse(
        u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), u.getStatusCadastro());
  }
}
