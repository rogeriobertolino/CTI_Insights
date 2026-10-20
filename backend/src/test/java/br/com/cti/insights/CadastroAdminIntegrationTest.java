package br.com.cti.insights;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.cti.insights.auth.*;
import br.com.cti.insights.auth.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "debug=false",
      "logging.level.root=WARN",
      "spring.datasource.url=jdbc:h2:mem:cadastros;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@AutoConfigureMockMvc
class CadastroAdminIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired UsuarioRepository users;
  @Autowired PasswordEncoder passwords;
  @Autowired JwtService jwt;
  @Autowired JdbcTemplate database;
  @Autowired ObjectMapper mapper;

  private Usuario conta(boolean admin, boolean pending) {
    var user =
        new Usuario(
            "Teste Cadastro",
            "cadastro-" + UUID.randomUUID() + "@example.com",
            passwords.encode("senha-teste-123"));
    if (pending) user.solicitarAprovacao();
    user = users.saveAndFlush(user);
    if (admin) {
      database.update("update usuarios set perfil='ADMIN' where id=?", user.getId());
      user = users.findById(user.getId()).orElseThrow();
    }
    return user;
  }

  private String loginBody(String email) {
    return "{\"email\":\"" + email + "\",\"senha\":\"senha-teste-123\"}";
  }

  private String authorization(Usuario user) {
    return "Bearer " + jwt.gerarToken(user);
  }

  private String decisao(String status) {
    return "{\"decisao\":\"" + status + "\"}";
  }

  @Test
  void cadastroPublicoSemprePendenteNaoPodeAutoAprovarOuVirarAdmin() throws Exception {
    String email = "publico-" + UUID.randomUUID() + "@example.com";
    String body =
        mapper.writeValueAsString(
            Map.of(
                "nome",
                "Solicitante",
                "email",
                email,
                "senha",
                "senha-teste-123",
                "perfil",
                "ADMIN",
                "statusCadastro",
                "APROVADO"));
    mvc.perform(post("/api/auth/cadastro").contentType("application/json").content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("perfil").value("USER"))
        .andExpect(jsonPath("statusCadastro").value("PENDENTE"))
        .andExpect(jsonPath("senhaHash").doesNotExist());
    mvc.perform(post("/api/auth/login").contentType("application/json").content(loginBody(email)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("token").doesNotExist());
    mvc.perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"senha\":\"errada\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/api/auth/register").contentType("application/json").content(body))
        .andExpect(status().isConflict());
  }

  @Test
  void apenasAdminListaERevisaCadastros() throws Exception {
    var pending = conta(false, true);
    var user = conta(false, false);
    mvc.perform(get("/api/admin/cadastros")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/admin/cadastros").header("Authorization", authorization(user)))
        .andExpect(status().isForbidden());
    mvc.perform(
            patch("/api/admin/cadastros/" + pending.getId())
                .header("Authorization", authorization(user))
                .contentType("application/json")
                .content(decisao("APROVADO")))
        .andExpect(status().isForbidden());
    // Mesmo um JWT assinado não supera um cadastro pendente.
    mvc.perform(get("/api/privado").header("Authorization", authorization(pending)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("code").value("CADASTRO_NAO_APROVADO"));
  }

  @Test
  void aprovacaoLiberaLoginERetornaAuditoriaSemSenhas() throws Exception {
    var admin = conta(true, false);
    var pending = conta(false, true);
    String auth = authorization(admin);
    var result =
        mvc.perform(
                get("/api/admin/cadastros")
                    .param("status", "PENDENTE")
                    .header("Authorization", auth))
            .andExpect(status().isOk())
            .andReturn();
    assertFalse(result.getResponse().getContentAsString().contains("senha"));
    mvc.perform(
            patch("/api/admin/cadastros/" + pending.getId())
                .header("Authorization", auth)
                .contentType("application/json")
                .content(decisao("APROVADO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("statusCadastro").value("APROVADO"))
        .andExpect(jsonPath("revisadoPor").value(admin.getEmail()))
        .andExpect(jsonPath("revisadoEm").isNotEmpty());
    var login =
        mvc.perform(
                post("/api/auth/login")
                    .contentType("application/json")
                    .content(loginBody(pending.getEmail())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("usuario.statusCadastro").value("APROVADO"))
            .andReturn();
    String token = mapper.readTree(login.getResponse().getContentAsString()).get("token").asText();
    mvc.perform(get("/api/privado").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    mvc.perform(
            patch("/api/admin/cadastros/" + pending.getId())
                .header("Authorization", auth)
                .contentType("application/json")
                .content(decisao("RECUSADO")))
        .andExpect(status().isConflict());
  }

  @Test
  void recusaBloqueiaLoginETokenExistente() throws Exception {
    var admin = conta(true, false);
    var pending = conta(false, true);
    String oldToken = authorization(pending);
    mvc.perform(
            patch("/api/admin/cadastros/" + pending.getId())
                .header("Authorization", authorization(admin))
                .contentType("application/json")
                .content(decisao("RECUSADO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("statusCadastro").value("RECUSADO"));
    mvc.perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content(loginBody(pending.getEmail())))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/auth/me").header("Authorization", oldToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void decisaoInvalidaCadastroInexistenteEContaAdminSaoProtegidos() throws Exception {
    var admin = conta(true, false);
    var pending = conta(false, true);
    String auth = authorization(admin);
    mvc.perform(
            patch("/api/admin/cadastros/" + pending.getId())
                .header("Authorization", auth)
                .contentType("application/json")
                .content(decisao("PENDENTE")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            patch("/api/admin/cadastros/" + pending.getId())
                .header("Authorization", auth)
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            patch("/api/admin/cadastros/" + admin.getId())
                .header("Authorization", auth)
                .contentType("application/json")
                .content(decisao("RECUSADO")))
        .andExpect(status().isForbidden());
    mvc.perform(
            patch("/api/admin/cadastros/999999999")
                .header("Authorization", auth)
                .contentType("application/json")
                .content(decisao("APROVADO")))
        .andExpect(status().isNotFound());
    mvc.perform(
            get("/api/admin/cadastros").param("status", "INVALIDO").header("Authorization", auth))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/admin/cadastros").param("pagina", "-1").header("Authorization", auth))
        .andExpect(status().isBadRequest());
  }

  @Test
  void cadastrosAnterioresContinuamAprovadosPeloDefault() {
    String email = "legado-" + UUID.randomUUID() + "@example.com";
    database.update(
        "insert into usuarios(nome,email,senha_hash) values(?,?,?)",
        "Usuário anterior",
        email,
        passwords.encode("senha-teste-123"));
    assertEquals(
        StatusCadastro.APROVADO, users.findByEmail(email).orElseThrow().getStatusCadastro());
  }
}
