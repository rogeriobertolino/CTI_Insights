package com.cti.provedor.service;

import com.cti.provedor.dto.CadastroAdminResponse;
import com.cti.provedor.dto.RevisaoCadastroRequest;
import com.cti.provedor.model.Perfil;
import com.cti.provedor.model.StatusCadastro;
import com.cti.provedor.repository.UsuarioRepository;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CadastroAdminService {
  private final UsuarioRepository users;

  public CadastroAdminService(UsuarioRepository users) {
    this.users = users;
  }

  public record PaginaCadastros(
      List<CadastroAdminResponse> content,
      long totalElements,
      int number,
      int totalPages,
      Map<StatusCadastro, Long> totais) {}

  @Transactional(readOnly = true)
  public PaginaCadastros listar(StatusCadastro status, int pagina) {
    if (pagina < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Página inválida.");
    var pageable = PageRequest.of(pagina, 25, Sort.by(Sort.Direction.DESC, "cadastradoEm", "id"));
    var result =
        status == null
            ? users.findByPerfil(Perfil.USER, pageable)
            : users.findByPerfilAndStatusCadastro(Perfil.USER, status, pageable);
    Map<StatusCadastro, Long> counts = new EnumMap<>(StatusCadastro.class);
    for (var state : StatusCadastro.values())
      counts.put(state, users.countByPerfilAndStatusCadastro(Perfil.USER, state));
    return new PaginaCadastros(
        result.getContent().stream().map(CadastroAdminResponse::from).toList(),
        result.getTotalElements(),
        result.getNumber(),
        result.getTotalPages(),
        counts);
  }

  @Transactional
  public CadastroAdminResponse revisar(
      Long id, RevisaoCadastroRequest request, String administrador) {
    if (request.decisao() == StatusCadastro.PENDENTE)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha aprovar ou recusar.");
    var user =
        users
            .findForReview(id)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Cadastro não encontrado."));
    if (user.getPerfil() != Perfil.USER || user.getEmail().equals(administrador))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Esta conta não pode ser alterada pela revisão de cadastros.");
    if (user.getStatusCadastro() != StatusCadastro.PENDENTE)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Este cadastro já foi analisado. Atualize a lista.");
    user.revisarCadastro(request.decisao(), administrador);
    return CadastroAdminResponse.from(users.saveAndFlush(user));
  }
}
