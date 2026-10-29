package com.cti.provedor.dto;

import com.cti.provedor.model.Perfil;
import com.cti.provedor.model.StatusCadastro;
import com.cti.provedor.model.Usuario;

public record UsuarioResponse(
    Long id, String nome, String email, Perfil perfil, StatusCadastro statusCadastro) {
  public static UsuarioResponse from(Usuario u) {
    return new UsuarioResponse(
        u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), u.getStatusCadastro());
  }
}
