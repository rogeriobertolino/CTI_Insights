package com.cti.provedor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cti.provedor.model.Usuario;
import com.cti.provedor.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:alterar-senha;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ChangePasswordIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired UsuarioRepository users;
  @Autowired PasswordEncoder passwords;

  @Test
  void loggedUserChangesPasswordOnlyWithCurrentPassword() throws Exception {
    String email = "troca-" + UUID.randomUUID() + "@example.com";
    users.saveAndFlush(new Usuario("Usuário Teste", email, passwords.encode("senha-antiga-123")));
    String token = login(email, "senha-antiga-123");

    mvc.perform(post("/api/auth/alterar-senha").contentType(MediaType.APPLICATION_JSON)
            .content(body("senha-antiga-123", "senha-nova-123")))
        .andExpect(status().isUnauthorized());

    mvc.perform(post("/api/auth/alterar-senha").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("senha-errada-123", "senha-nova-123")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("message").value("A senha atual está incorreta."));

    mvc.perform(post("/api/auth/alterar-senha").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("senha-antiga-123", "senha-antiga-123")))
        .andExpect(status().isBadRequest());

    mvc.perform(post("/api/auth/alterar-senha").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("senha-antiga-123", "senha-nova-123")))
        .andExpect(status().isNoContent());

    assertTrue(passwords.matches("senha-nova-123", users.findByEmail(email).orElseThrow().getSenhaHash()));
    login(email, "senha-nova-123");
  }

  private String body(String atual, String nova) throws Exception {
    return mapper.writeValueAsString(Map.of("senhaAtual", atual, "novaSenha", nova));
  }

  private String login(String email, String senha) throws Exception {
    String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("email", email, "senha", senha))))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    return mapper.readTree(response).get("token").asText();
  }
}
