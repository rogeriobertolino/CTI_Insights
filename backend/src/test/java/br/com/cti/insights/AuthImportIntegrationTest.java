package br.com.cti.insights;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.cti.insights.auth.UsuarioRepository;
import br.com.cti.insights.imports.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:cti;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@AutoConfigureMockMvc
class AuthImportIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired UsuarioRepository users;
  @Autowired ObjectMapper mapper;
  @Autowired br.com.cti.insights.clients.ConsultorRepository consultores;
  @Autowired br.com.cti.insights.clients.ClienteRepository clientes;
  @Autowired ImportDomainService domain;
  @MockBean PythonAnalysisService python;

  @Test
  void cadastroLoginJwtLogoutEProtecao() throws Exception {
    mvc.perform(get("/api/privado")).andExpect(status().isUnauthorized());
    String email = "teste-" + UUID.randomUUID() + "@example.com";
    String data =
        mapper.writeValueAsString(
            Map.of(
                "nome",
                "Teste Usuário",
                "email",
                email,
                "senha",
                "senha-teste-123",
                "perfil",
                "ADMIN"));
    mvc.perform(post("/api/auth/cadastro").contentType("application/json").content(data))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("perfil").value("USER"))
        .andExpect(jsonPath("senhaHash").doesNotExist())
        .andExpect(jsonPath("senha").doesNotExist());
    assertTrue(
        passwords.matches(
            "senha-teste-123", users.findByEmail(email).orElseThrow().getSenhaHash()));
    mvc.perform(post("/api/auth/register").contentType("application/json").content(data))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/auth/cadastro")
                .contentType("application/json")
                .content("{\"nome\":\"A\",\"email\":\"bad\",\"senha\":\"1\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("email", email, "senha", "errada"))))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content(
                    mapper.writeValueAsString(Map.of("email", email, "senha", "senha-teste-123"))))
        .andExpect(status().isForbidden());
    database.update("update usuarios set status_cadastro='APROVADO' where email=?", email);
    var result =
        mvc.perform(
                post("/api/auth/login")
                    .contentType("application/json")
                    .content(
                        mapper.writeValueAsString(
                            Map.of(
                                "email",
                                email.toUpperCase(Locale.ROOT),
                                "senha",
                                "senha-teste-123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("tipo").value("Bearer"))
            .andExpect(jsonPath("expiraEmSegundos").value(1800))
            .andExpect(jsonPath("usuario.email").value(email))
            .andExpect(jsonPath("usuario.senhaHash").doesNotExist())
            .andReturn();
    assertNull(result.getRequest().getSession(false));
    assertNull(result.getResponse().getHeader("Set-Cookie"));
    String token = mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    var claims = jwt.validarToken(token);
    assertEquals(email, claims.getSubject());
    assertEquals("USER", claims.get("perfil"));
    assertNull(claims.get("senha"));
    assertNull(claims.get("senhaHash"));
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("email").value(email));
    mvc.perform(get("/api/privado").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/privado")
                .cookie(new jakarta.servlet.http.Cookie("JSESSIONID", "sessao-antiga")))
        .andExpect(status().isUnauthorized());
  }

  @Autowired br.com.cti.insights.auth.security.JwtService jwt;
  @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;
  @Autowired org.springframework.jdbc.core.JdbcTemplate database;

  @org.springframework.beans.factory.annotation.Value("${cti.jwt.secret-base64}")
  String testSecret;

  @org.springframework.beans.factory.annotation.Value("${cti.jwt.issuer}")
  String testIssuer;

  private br.com.cti.insights.auth.Usuario novoUsuario() {
    return users.saveAndFlush(
        new br.com.cti.insights.auth.Usuario(
            "Teste JWT",
            "jwt-" + UUID.randomUUID() + "@example.com",
            passwords.encode("senha-teste-123")));
  }

  private String tokenAssinado(
      br.com.cti.insights.auth.Usuario user,
      String issuer,
      java.util.Date expiration,
      javax.crypto.SecretKey key,
      boolean includeUid) {
    var builder =
        io.jsonwebtoken.Jwts.builder()
            .issuer(issuer)
            .subject(user.getEmail())
            .claim("perfil", "ADMIN");
    if (includeUid) builder.claim("uid", user.getId());
    if (expiration != null) builder.expiration(expiration);
    return builder.signWith(key, io.jsonwebtoken.Jwts.SIG.HS256).compact();
  }

  @Test
  void rejeitaTokenExpiradoAssinaturaInvalidaClaimsAusentesEEmissorErrado() throws Exception {
    var user = novoUsuario();
    var key =
        io.jsonwebtoken.security.Keys.hmacShaKeyFor(
            io.jsonwebtoken.io.Decoders.BASE64.decode(testSecret));
    var future = java.util.Date.from(java.time.Instant.now().plusSeconds(300));
    var past = java.util.Date.from(java.time.Instant.now().minusSeconds(30));
    var otherKey = io.jsonwebtoken.Jwts.SIG.HS256.key().build();
    for (String token :
        List.of(
            "invalido",
            tokenAssinado(user, testIssuer, past, key, true),
            tokenAssinado(user, testIssuer, future, otherKey, true),
            tokenAssinado(user, "outro-emissor", future, key, true),
            tokenAssinado(user, testIssuer, null, key, true),
            tokenAssinado(user, testIssuer, future, key, false))) {
      mvc.perform(get("/api/privado").header("Authorization", "Bearer " + token))
          .andExpect(status().isUnauthorized());
    }
    mvc.perform(get("/api/privado").header("Authorization", "Basic qualquer"))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/privado").header("Authorization", "Bearer "))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void perfilAdminExigePermissaoPersistidaENaoApenasClaim() throws Exception {
    var user = novoUsuario();
    var key =
        io.jsonwebtoken.security.Keys.hmacShaKeyFor(
            io.jsonwebtoken.io.Decoders.BASE64.decode(testSecret));
    String claimedAdmin =
        tokenAssinado(
            user,
            testIssuer,
            java.util.Date.from(java.time.Instant.now().plusSeconds(300)),
            key,
            true);
    mvc.perform(get("/api/admin").header("Authorization", "Bearer " + claimedAdmin))
        .andExpect(status().isForbidden());
    database.update("update usuarios set perfil='ADMIN' where id=?", user.getId());
    mvc.perform(get("/api/admin").header("Authorization", "Bearer " + claimedAdmin))
        .andExpect(status().isOk());
    mvc.perform(get("/api/privado").header("Authorization", "Bearer " + claimedAdmin))
        .andExpect(status().isOk());
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + claimedAdmin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("perfil").value("ADMIN"));
    database.update("update usuarios set perfil='USER' where id=?", user.getId());
    mvc.perform(get("/api/admin").header("Authorization", "Bearer " + claimedAdmin))
        .andExpect(status().isForbidden());
  }

  @Test
  void tokenDeUsuarioRemovidoNaoAutentica() throws Exception {
    var user = novoUsuario();
    String token = jwt.gerarToken(user);
    users.delete(user);
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void colunaPerfilTemDefaultCompativelComCadastrosAntigos() {
    String email = "legado-" + UUID.randomUUID() + "@example.com";
    database.update(
        "insert into usuarios (nome,email,senha_hash) values (?,?,?)",
        "Usuário antigo",
        email,
        passwords.encode("senha-antiga-123"));
    assertEquals(
        br.com.cti.insights.auth.Perfil.USER, users.findByEmail(email).orElseThrow().getPerfil());
  }

  @Test
  void configuracaoRecusaSegredoAusenteCurtoEExpiracaoInvalida() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new br.com.cti.insights.auth.security.JwtService("", testIssuer, 1800));
    assertThrows(
        IllegalArgumentException.class,
        () -> new br.com.cti.insights.auth.security.JwtService("YWJj", testIssuer, 1800));
    assertThrows(
        IllegalArgumentException.class,
        () -> new br.com.cti.insights.auth.security.JwtService(testSecret, testIssuer, 0));
  }

  @Test
  void importacaoValidaPersisteERetornaDashboard() throws Exception {
    String authToken = jwt.gerarToken(novoUsuario());
    Map<String, Object> row =
        new LinkedHashMap<>(
            Map.of(
                "codigo_cliente",
                "CLI-" + UUID.randomUUID(),
                "nome_cliente",
                "Empresa 001",
                "segmento",
                "Serviços",
                "nivel",
                "A",
                "faturamento_mensal",
                "1234.56",
                "linha_origem",
                "12"));
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("records", List.of(row));
    result.put("headers", new ArrayList<>(row.keySet()));
    result.put("totalRows", 1);
    result.put("quality", 100.0);
    result.put("columns", Map.of("clientCode", 0, "clientName", 1, "segment", 2));
    when(python.analyze(any(Path.class))).thenReturn(result);
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "validado.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new byte[] {1});
    mvc.perform(
            multipart("/api/v1/imports").file(file).header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status").value("COMPLETED"));
    mvc.perform(get("/api/v1/dashboard/latest").header("Authorization", "Bearer " + authToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("totalRows").value(1));
  }

  @Test
  void reutilizaConsultorExistenteComCaixaAcentosEEspacosDiferentes() {
    var existente =
        consultores.saveAndFlush(
            new br.com.cti.insights.domain.Consultor("ÁNA   SOUZA", "anasouza@cti.local", null));
    long quantidade = consultores.count();
    String prefix = "CONSULTOR-" + UUID.randomUUID();
    var records =
        java.util.stream.IntStream.range(0, 3)
            .mapToObj(
                i ->
                    new LinkedHashMap<String, Object>(
                        Map.of(
                            "codigo_cliente",
                            prefix + "-" + i,
                            "nome_cliente",
                            "Empresa " + i,
                            "segmento",
                            "Serviços",
                            "consultor",
                            List.of("Ana Souza", "ANA SOUZA", "Ana   Souza").get(i),
                            "nivel",
                            "A",
                            "faturamento",
                            100)))
            .toList();
    assertEquals(3, domain.persistClients(new ArrayList<>(records)));
    assertEquals(quantidade, consultores.count());
    assertTrue(
        clientes.findAll().stream()
            .filter(c -> c.getCodigoCti().startsWith(prefix))
            .allMatch(c -> c.getConsultor().getId().equals(existente.getId())));
  }

  @Test
  void servidorRejeitaLinhasInvalidasMesmoSemValidacaoFrontend() throws Exception {
    ImportValidationService validation = new ImportValidationService();
    Map<String, Object> row =
        new HashMap<>(
            Map.of(
                "codigo_cliente",
                "1",
                "nome_cliente",
                "Empresa",
                "segmento",
                "Serviços",
                "nivel",
                "A",
                "faturamento",
                -1));
    assertThrows(ExcelInvalidoException.class, () -> validation.validate(List.of(row)));
    row.put("faturamento", 100);
    assertThrows(ExcelInvalidoException.class, () -> validation.validate(List.of(row, row)));
    row.put("nivel", "D");
    assertThrows(ExcelInvalidoException.class, () -> validation.validate(List.of(row)));
  }
}
