package com.cti.provedor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cti.provedor.model.Usuario;
import com.cti.provedor.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:reset;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.mail.host=mail.test",
    "management.health.mail.enabled=false",
    "cti.auth.reset-url=http://localhost:5173/redefinir-senha"
})
@AutoConfigureMockMvc
class PasswordResetIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired UsuarioRepository users;
  @Autowired PasswordEncoder passwords;
  @MockBean JavaMailSender mailSender;

  @Test
  void recoveryIsGenericAndTokenCanOnlyResetPasswordOnce() throws Exception {
    String email = "reset-" + UUID.randomUUID() + "@example.com";
    users.saveAndFlush(new Usuario("Usuário Teste", email, passwords.encode("senha-antiga-123")));

    String genericMessage = "Se o e-mail estiver cadastrado, enviaremos um link para redefinir sua senha.";
    mvc.perform(post("/api/auth/recuperar-senha")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("email", email))))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("message").value(genericMessage));

    var message = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(message.capture());
    var matcher = Pattern.compile("[?&]token=([A-Za-z0-9_-]+)").matcher(message.getValue().getText());
    assertTrue(matcher.find());
    String token = matcher.group(1);
    String request = mapper.writeValueAsString(Map.of("token", token, "senha", "senha-nova-123"));

    mvc.perform(post("/api/auth/redefinir-senha").contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isNoContent());
    assertTrue(passwords.matches("senha-nova-123", users.findByEmail(email).orElseThrow().getSenhaHash()));
    mvc.perform(post("/api/auth/redefinir-senha").contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isBadRequest());

    String unknown = "ausente-" + UUID.randomUUID() + "@example.com";
    mvc.perform(post("/api/auth/recuperar-senha").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("email", unknown))))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("message").value(genericMessage));
  }
}