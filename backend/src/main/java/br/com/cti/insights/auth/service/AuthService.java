package br.com.cti.insights.auth.service;

import br.com.cti.insights.auth.*;
import br.com.cti.insights.auth.dto.*;
import br.com.cti.insights.auth.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
  private final UsuarioRepository users;
  private final PasswordEncoder passwords;
  private final JwtService jwt;
  private final String dummyHash;

  public AuthService(UsuarioRepository users, PasswordEncoder passwords, JwtService jwt) {
    this.users = users;
    this.passwords = passwords;
    this.jwt = jwt;
    dummyHash = passwords.encode(java.util.UUID.randomUUID().toString());
  }

  @Transactional
  public UsuarioResponse cadastrar(CadastroRequest request) {
    String email = normalize(request.email());
    if (request.nome().trim().length() < 3
        || request.senha().getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Confira o nome e use uma senha de 8 a 72 bytes.");
    if (users.existsByEmail(email))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado.");
    // O cadastro público sempre cria USER; nenhum perfil é recebido do cliente.
    var user = new Usuario(request.nome().trim(), email, passwords.encode(request.senha()));
    user.solicitarAprovacao();
    return UsuarioResponse.from(users.saveAndFlush(user));
  }

  @Transactional(readOnly = true)
  public LoginResponse login(LoginRequest request) {
    Usuario user = users.findByEmail(normalize(request.email())).orElse(null);
    boolean matches =
        request.senha().getBytes(StandardCharsets.UTF_8).length <= 72
            && passwords.matches(request.senha(), user == null ? dummyHash : user.getSenhaHash());
    if (user == null || !matches || request.senha().getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
    if (user.getStatusCadastro() != StatusCadastro.APROVADO)
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          user.getStatusCadastro() == StatusCadastro.PENDENTE
              ? "Seu cadastro está aguardando aprovação do administrador."
              : "Seu cadastro não foi autorizado. Entre em contato com o administrador.");
    return new LoginResponse(
        jwt.gerarToken(user), "Bearer", jwt.expiracaoSegundos(), UsuarioResponse.from(user));
  }

  @Transactional(readOnly = true)
  public UsuarioResponse usuario(String email) {
    return UsuarioResponse.from(
        users
            .findByEmail(email)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Entre na sua conta para continuar.")));
  }

  private String normalize(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
