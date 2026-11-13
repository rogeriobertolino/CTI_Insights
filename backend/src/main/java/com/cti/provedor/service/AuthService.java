package com.cti.provedor.service;

import com.cti.provedor.security.JwtService;
import com.cti.provedor.dto.AlterarSenhaRequest;
import com.cti.provedor.dto.CadastroRequest;
import com.cti.provedor.dto.LoginRequest;
import com.cti.provedor.dto.LoginResponse;
import com.cti.provedor.dto.RedefinirSenhaRequest;
import com.cti.provedor.dto.SolicitarRedefinicaoSenhaRequest;
import com.cti.provedor.dto.UsuarioResponse;
import com.cti.provedor.model.StatusCadastro;
import com.cti.provedor.model.Usuario;
import com.cti.provedor.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
  private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
  private static final SecureRandom secureRandom = new SecureRandom();
  private static final String RESET_MESSAGE =
      "Se o e-mail estiver cadastrado, enviaremos um link para redefinir sua senha.";
  private final UsuarioRepository users;
  private final PasswordEncoder passwords;
  private final JwtService jwt;
  private final ObjectProvider<JavaMailSender> mailSenders;
  private final String resetUrl;
  private final String mailFrom;
  private final String smtpHost;
  private final String dummyHash;
  private final boolean cadastroRequerAprovacao;

  public AuthService(
      UsuarioRepository users,
      PasswordEncoder passwords,
      JwtService jwt,
      ObjectProvider<JavaMailSender> mailSenders,
      @Value("${cti.auth.reset-url:http://localhost:5173/redefinir-senha}") String resetUrl,
      @Value("${cti.auth.mail-from:no-reply@cti-insights.local}") String mailFrom,
      @Value("${spring.mail.host:}") String smtpHost,
      @Value("${cti.auth.cadastro-requer-aprovacao:true}") boolean cadastroRequerAprovacao) {
    this.cadastroRequerAprovacao = cadastroRequerAprovacao;
    this.users = users;
    this.passwords = passwords;
    this.jwt = jwt;
    this.mailSenders = mailSenders;
    this.resetUrl = resetUrl;
    this.mailFrom = mailFrom;
    this.smtpHost = smtpHost;
    dummyHash = passwords.encode(java.util.UUID.randomUUID().toString());
  }

  @Transactional
  public String solicitarRedefinicaoSenha(SolicitarRedefinicaoSenhaRequest request) {
    var user = users.findByEmail(normalize(request.email())).orElse(null);
    JavaMailSender mailSender = smtpHost.isBlank() ? null : mailSenders.getIfAvailable();
    if (user == null || mailSender == null) {
      return RESET_MESSAGE;
    }

    OffsetDateTime now = OffsetDateTime.now();
    if (user.getResetPasswordRequestedAt() != null
        && user.getResetPasswordRequestedAt().isAfter(now.minusMinutes(1))) return RESET_MESSAGE;

    byte[] randomToken = new byte[32];
    secureRandom.nextBytes(randomToken);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);
    user.iniciarRedefinicaoSenha(hashToken(token), now.plusMinutes(30), now);
    users.save(user);

    var message = new SimpleMailMessage();
    message.setFrom(mailFrom);
    message.setTo(user.getEmail());
    message.setSubject("Redefinição de senha | CTI Insights");
    String separator = resetUrl.contains("?") ? "&" : "?";
    message.setText(
        "Recebemos uma solicitação para redefinir a senha da sua conta.\n\n"
            + resetUrl + separator + "token=" + token
            + "\n\nO link expira em 30 minutos e só pode ser usado uma vez."
            + " Se você não solicitou a redefinição, ignore esta mensagem.");
    try {
      mailSender.send(message);
    } catch (RuntimeException error) {
      user.limparRedefinicaoSenha();
      users.save(user);
      logger.warn("Não foi possível enviar mensagem de redefinição de senha.");
    }
    return RESET_MESSAGE;
  }

  @Transactional
  public void redefinirSenha(RedefinirSenhaRequest request) {
    var user = users.findByResetPasswordTokenHashForUpdate(hashToken(request.token()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "O link expirou ou é inválido. Solicite uma nova redefinição."));
    if (user.getResetPasswordExpiresAt() == null
        || !user.getResetPasswordExpiresAt().isAfter(OffsetDateTime.now())) {
      user.limparRedefinicaoSenha();
      users.save(user);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "O link expirou ou é inválido. Solicite uma nova redefinição.");
    }
    if (request.senha().getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter até 72 bytes.");
    user.redefinirSenha(passwords.encode(request.senha()));
    users.save(user);
  }

  // Responde 400 (não 401) para a senha atual errada: o 401 encerraria a sessão no front-end.
  @Transactional
  public void alterarSenha(String email, AlterarSenhaRequest request) {
    var user = users.findByEmail(normalize(email))
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.UNAUTHORIZED, "Entre na sua conta para continuar."));
    if (request.senhaAtual().getBytes(StandardCharsets.UTF_8).length > 72
        || !passwords.matches(request.senhaAtual(), user.getSenhaHash()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha atual está incorreta.");
    if (request.novaSenha().getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter até 72 bytes.");
    if (request.novaSenha().equals(request.senhaAtual()))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A nova senha deve ser diferente da atual.");
    user.redefinirSenha(passwords.encode(request.novaSenha()));
    users.save(user);
  }

  private String hashToken(String token) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 indisponível.", error);
    }
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
    user.setDadosAcademicos(request.matricula(), request.turma());
    // Com aprovação desligada (CADASTRO_REQUER_APROVACAO=false) o login funciona logo após o
    // cadastro, como no roteiro de testes da Aula 10.
    if (cadastroRequerAprovacao) user.solicitarAprovacao();
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
