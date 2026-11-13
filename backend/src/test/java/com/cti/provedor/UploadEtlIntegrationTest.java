package com.cti.provedor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cti.provedor.model.EstatisticaLote;
import com.cti.provedor.model.Usuario;
import com.cti.provedor.repository.EstatisticaLoteRepository;
import com.cti.provedor.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Fluxo da Aula 12 com um servidor HTTP falso no lugar do FastAPI. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:upload-etl;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "cti.etl.api-key=segredo-de-teste"
})
@AutoConfigureMockMvc
class UploadEtlIntegrationTest {
  static final HttpServer PYTHON;
  static final AtomicInteger SAUDE = new AtomicInteger(200);
  static final AtomicInteger ETL_STATUS = new AtomicInteger(200);
  static final AtomicReference<String> ETL_CORPO = new AtomicReference<>();
  static final AtomicReference<String> ETL_CHAVE = new AtomicReference<>();

  static {
    try {
      PYTHON = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    } catch (IOException error) {
      throw new IllegalStateException(error);
    }
    PYTHON.createContext("/saude", troca -> {
      troca.sendResponseHeaders(SAUDE.get(), -1);
      troca.close();
    });
    PYTHON.createContext("/etl", troca -> {
      ETL_CORPO.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      ETL_CHAVE.set(troca.getRequestHeaders().getFirst("X-ETL-Key"));
      byte[] corpo = (ETL_STATUS.get() == 200 ? "{\"ok\":true}" : "{\"detail\":\"Falha no ETL: coluna quebrada\"}")
          .getBytes(StandardCharsets.UTF_8);
      troca.sendResponseHeaders(ETL_STATUS.get(), corpo.length);
      troca.getResponseBody().write(corpo);
      troca.close();
    });
    PYTHON.start();
  }

  @DynamicPropertySource
  static void etl(DynamicPropertyRegistry registro) {
    registro.add("cti.etl.url", () -> "http://127.0.0.1:" + PYTHON.getAddress().getPort() + "/etl");
  }

  @AfterAll
  static void parar() {
    PYTHON.stop(0);
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired UsuarioRepository usuarios;
  @Autowired PasswordEncoder senhas;
  @Autowired EstatisticaLoteRepository estatisticas;
  @Autowired JdbcTemplate jdbc;
  String token;

  @BeforeEach
  void entrar() throws Exception {
    SAUDE.set(200);
    ETL_STATUS.set(200);
    ETL_CORPO.set(null);
    String email = "upload-" + UUID.randomUUID() + "@example.com";
    usuarios.saveAndFlush(new Usuario("Aluno Upload", email, senhas.encode("senha-forte-123")));
    String resposta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("email", email, "senha", "senha-forte-123"))))
        .andReturn().getResponse().getContentAsString();
    token = mapper.readTree(resposta).get("token").asText();
  }

  @Test
  void enviaSoAsLinhasValidasAoPythonDepoisDoCommitEDashboardLeOResultado() throws Exception {
    var linhas = List.of(
        linha(2, " cti001 ", "A", "1850000"),
        linha(3, "CTI002", "b", "780000"),
        linha(4, "CTI003", "X", "100"));            // nível inválido: o backend recusa
    JsonNode resposta = enviar(pedido(4, 1, linhas), 201);

    assertEquals(1, resposta.at("/rejeitadas").size());
    assertEquals(4, resposta.at("/rejeitadas/0/linha").asInt());
    assertEquals(2, resposta.at("/lote/linhasValidas").asInt());
    assertEquals(2, resposta.at("/lote/linhasInvalidas").asInt());
    // O Python falso não grava no banco: o lote continua como o Spring deixou.
    assertEquals("PENDENTE", resposta.at("/lote/status").asText());

    JsonNode enviado = mapper.readTree(ETL_CORPO.get());
    long loteId = resposta.at("/lote/id").asLong();
    assertEquals(loteId, enviado.get("loteId").asLong());
    assertEquals(4, enviado.get("recebidas").asInt());
    assertEquals(2, enviado.get("linhas").size());
    assertEquals("CTI001", enviado.at("/linhas/0/codigoCliente").asText());
    assertEquals("B", enviado.at("/linhas/1/nivelCliente").asText());
    assertEquals("segredo-de-teste", ETL_CHAVE.get());

    // O Python gravaria isto (banco.py):
    jdbc.update("update upload_lote set status = 'PROCESSADO' where id = ?", loteId);
    estatisticas.saveAndFlush(new EstatisticaLote(loteId, "{\"resumo\":{\"media\":2261250.0}}"));
    mvc.perform(get("/api/dashboard").param("loteId", String.valueOf(loteId)).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lote.status").value("PROCESSADO"))
        .andExpect(jsonPath("$.estatisticas.resumo.media").value(2261250.0));
    mvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void pythonDesligadoDeixaLoteComErroEDashboardRecusa() throws Exception {
    SAUDE.set(503);
    JsonNode resposta = enviar(pedido(1, 0, List.of(linha(2, "CTI010", "A", "500000"))), 201);
    assertEquals("ERRO", resposta.at("/lote/status").asText());
    assertTrue(resposta.at("/lote/mensagem").asText().contains("Não foi possível falar com o serviço Python"));
    assertEquals(null, ETL_CORPO.get());
    mvc.perform(get("/api/dashboard").param("loteId", resposta.at("/lote/id").asText())
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict());
  }

  @Test
  void erroDoPythonApareceNaMensagemDoLote() throws Exception {
    ETL_STATUS.set(500);
    JsonNode resposta = enviar(pedido(1, 0, List.of(linha(2, "CTI020", "C", "90000"))), 201);
    assertEquals("ERRO", resposta.at("/lote/status").asText());
    assertTrue(resposta.at("/lote/mensagem").asText().contains("coluna quebrada"));
  }

  @Test
  void exigeLoginEContagensCoerentes() throws Exception {
    mvc.perform(post("/api/uploads/validadas").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(pedido(1, 0, List.of(linha(2, "CTI030", "A", "1"))))))
        .andExpect(status().isUnauthorized());
    enviar(pedido(5, 0, List.of(linha(2, "CTI031", "A", "1"))), 400);
    enviar(pedido(1, 0, List.of(linha(2, "CTI032", "Z", "1"))), 422);
  }

  private JsonNode enviar(Map<String, Object> pedido, int esperado) throws Exception {
    String corpo = mvc.perform(post("/api/uploads/validadas").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(pedido)))
        .andExpect(status().is(esperado))
        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    return mapper.readTree(corpo);
  }

  private Map<String, Object> pedido(int recebidas, int invalidas, List<Map<String, Object>> linhas) {
    Map<String, Object> pedido = new LinkedHashMap<>();
    pedido.put("arquivoNome", "dados.xlsx");
    pedido.put("recebidas", recebidas);
    pedido.put("invalidas", invalidas);
    pedido.put("comAviso", 0);
    pedido.put("linhas", linhas);
    return pedido;
  }

  private Map<String, Object> linha(int numero, String codigo, String nivel, String faturamento) {
    Map<String, Object> linha = new LinkedHashMap<>();
    linha.put("linha", numero);
    linha.put("codigoCliente", codigo);
    linha.put("nomeCliente", "Empresa " + codigo.trim());
    linha.put("consultor", "ana souza");
    linha.put("segmento", "IND.");
    linha.put("nivelCliente", nivel);
    linha.put("faturamentoAnual", new java.math.BigDecimal(faturamento));
    linha.put("servicosContratados", "Internet Dedicada;Firewall");
    linha.put("dataContratacao", "2025-01-15");
    linha.put("cidade", "Campinas");
    linha.put("uf", "sp");
    return linha;
  }
}
