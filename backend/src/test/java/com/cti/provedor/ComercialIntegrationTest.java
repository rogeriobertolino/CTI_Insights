package com.cti.provedor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cti.provedor.security.JwtService;
import com.cti.provedor.model.Usuario;
import com.cti.provedor.repository.ClienteRepository;
import com.cti.provedor.repository.UsuarioRepository;
import com.cti.provedor.service.PythonAnalysisService;
import com.fasterxml.jackson.databind.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "debug=false",
      "logging.level.root=WARN",
      "spring.datasource.url=jdbc:h2:mem:comercial;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@AutoConfigureMockMvc
class ComercialIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired UsuarioRepository users;
  @Autowired PasswordEncoder passwords;
  @Autowired JwtService jwt;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate database;
  @Autowired ClienteRepository clientes;
  @MockBean PythonAnalysisService python;

  private String auth(boolean admin) {
    var user =
        users.saveAndFlush(
            new Usuario(
                "Teste comercial",
                "comercial-" + UUID.randomUUID() + "@example.com",
                passwords.encode("senha-teste-123")));
    if (admin) {
      database.update("update usuarios set perfil='ADMIN' where id=?", user.getId());
      user = users.findById(user.getId()).orElseThrow();
    }
    return "Bearer " + jwt.gerarToken(user);
  }

  private JsonNode create(String resource, Object data, String auth) throws Exception {
    var result =
        mvc.perform(
                post("/api/v1/" + resource)
                    .header("Authorization", auth)
                    .contentType("application/json")
                    .content(mapper.writeValueAsBytes(data)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andReturn();
    return mapper.readTree(result.getResponse().getContentAsString());
  }

  private long consultor(String auth) throws Exception {
    return create(
            "consultores",
            Map.of(
                "nome",
                "Consultor teste",
                "email",
                "consultor-" + UUID.randomUUID() + "@example.com"),
            auth)
        .get("id")
        .asLong();
  }

  private Map<String, Object> client(long consultor, String code) {
    return new LinkedHashMap<>(
        Map.of(
            "codigoCti",
            code,
            "consultorId",
            consultor,
            "nomeEmpresa",
            "Empresa teste",
            "segmento",
            "Serviços",
            "faturamentoAnual",
            1200,
            "nivel",
            "A",
            "status",
            "ATIVO"));
  }

  private long cliente(String auth, long consultor) throws Exception {
    return create("clientes", client(consultor, "CLI-" + UUID.randomUUID()), auth)
        .get("id")
        .asLong();
  }

  private Map<String, Object> contract(long client, long service) {
    return new LinkedHashMap<>(
        Map.of(
            "clienteId",
            client,
            "servicoId",
            service,
            "dataInicio",
            "2026-10-01",
            "dataFim",
            "2026-12-01",
            "valor",
            123.45,
            "status",
            "ATIVO"));
  }

  @Test
  void protegeNovosRecursosEImpedeEscritaDeUsuarioComum() throws Exception {
    String user = auth(false);
    for (String resource : List.of("consultores", "servicos", "contratos", "insights")) {
      mvc.perform(get("/api/v1/" + resource)).andExpect(status().isUnauthorized());
      mvc.perform(get("/api/v1/" + resource).header("Authorization", user))
          .andExpect(status().isOk());
      mvc.perform(
              post("/api/v1/" + resource)
                  .header("Authorization", user)
                  .contentType("application/json")
                  .content("{}"))
          .andExpect(status().isForbidden());
    }
    mvc.perform(get("/api/admin/telemetrias").header("Authorization", user))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/dominios")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/dominios").header("Authorization", user))
        .andExpect(status().isOk())
        .andExpect(jsonPath("niveis[0]").value("A"));
    mvc.perform(get("/servicos")).andExpect(status().isNotFound());
  }

  @Test
  void clienteInexistenteRetorna404EErrosPossuemFormatoComum() throws Exception {
    String admin = auth(true);
    mvc.perform(get("/api/clientes/99999999").header("Authorization", admin))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("status").value(404))
        .andExpect(jsonPath("timestamp").exists())
        .andExpect(jsonPath("fields").isArray());
    long consultant = consultor(admin);
    var body = client(consultant, "X".repeat(81));
    mvc.perform(
            post("/api/clientes")
                .header("Authorization", admin)
                .contentType("application/json")
                .content(mapper.writeValueAsBytes(body)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("fields[0].field").value("codigoCti"));
    body = client(consultant, "OK");
    body.put("consultorId", 0);
    mvc.perform(
            post("/api/clientes")
                .header("Authorization", admin)
                .contentType("application/json")
                .content(mapper.writeValueAsBytes(body)))
        .andExpect(status().isBadRequest());
    body = client(999999999, "OK");
    mvc.perform(
            post("/api/clientes")
                .header("Authorization", admin)
                .contentType("application/json")
                .content(mapper.writeValueAsBytes(body)))
        .andExpect(status().isNotFound());
  }

  @Test
  void consultorAtualizaDadosNormalizaEmailENaoExpoeSenha() throws Exception {
    String admin = auth(true);
    String email = "teste-" + UUID.randomUUID() + "@example.com";
    var created =
        create(
            "consultores",
            Map.of(
                "nome",
                "Nome teste",
                "email",
                email.toUpperCase(Locale.ROOT),
                "telefone",
                "123",
                "matricula",
                "M1"),
            admin);
    assertEquals(email, created.get("email").asText());
    assertFalse(created.has("senha"));
    assertFalse(created.has("senhaHash"));
    long id = created.get("id").asLong();
    mvc.perform(
            put("/api/v1/consultores/" + id)
                .header("Authorization", admin)
                .contentType("application/json")
                .content(
                    mapper.writeValueAsBytes(
                        Map.of(
                            "nome",
                            "Nome atualizado",
                            "email",
                            email,
                            "telefone",
                            "456",
                            "matricula",
                            "M2"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("telefone").value("456"))
        .andExpect(jsonPath("matricula").value("M2"));
    mvc.perform(
            post("/api/v1/consultores")
                .header("Authorization", admin)
                .contentType("application/json")
                .content(
                    mapper.writeValueAsBytes(
                        Map.of("nome", "Duplicado", "email", email.toUpperCase(Locale.ROOT)))))
        .andExpect(status().isConflict());
    mvc.perform(delete("/api/v1/consultores/" + id).header("Authorization", admin))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/consultores/" + id).header("Authorization", admin))
        .andExpect(status().isNotFound());
  }

  @Test
  void contratosEInsightsValidamVinculosEDatasEProtegemExclusoes() throws Exception {
    String admin = auth(true);
    long consultant = consultor(admin);
    long first = cliente(admin, consultant);
    long second = cliente(admin, consultant);
    long service =
        create("servicos", Map.of("nome", "Serviço teste", "preco", 12.34), admin)
            .get("id")
            .asLong();
    var body = contract(first, service);
    body.put("dataFim", "2026-09-01");
    mvc.perform(
            post("/api/v1/contratos")
                .header("Authorization", admin)
                .contentType("application/json")
                .content(mapper.writeValueAsBytes(body)))
        .andExpect(status().isBadRequest());
    body = contract(first, 99999999);
    mvc.perform(
            post("/api/v1/contratos")
                .header("Authorization", admin)
                .contentType("application/json")
                .content(mapper.writeValueAsBytes(body)))
        .andExpect(status().isNotFound());
    long contract = create("contratos", contract(first, service), admin).get("id").asLong();
    mvc.perform(
            post("/api/v1/insights")
                .header("Authorization", admin)
                .contentType("application/json")
                .content(
                    mapper.writeValueAsBytes(
                        Map.of(
                            "clienteId",
                            second,
                            "contratoId",
                            contract,
                            "tipo",
                            "MANUAL",
                            "descricao",
                            "Teste"))))
        .andExpect(status().isBadRequest());
    var insight =
        create(
            "insights",
            Map.of("contratoId", contract, "tipo", "MANUAL", "descricao", "Teste"),
            admin);
    assertEquals(first, insight.get("clienteId").asLong());
    mvc.perform(delete("/api/v1/consultores/" + consultant).header("Authorization", admin))
        .andExpect(status().isConflict());
    mvc.perform(delete("/api/v1/clientes/" + first).header("Authorization", admin))
        .andExpect(status().isConflict());
    mvc.perform(delete("/api/v1/servicos/" + service).header("Authorization", admin))
        .andExpect(status().isConflict());
    mvc.perform(delete("/api/v1/contratos/" + contract).header("Authorization", admin))
        .andExpect(status().isConflict());
    mvc.perform(
            put("/api/v1/contratos/" + contract)
                .header("Authorization", admin)
                .contentType("application/json")
                .content(mapper.writeValueAsBytes(contract(second, service))))
        .andExpect(status().isConflict());
    mvc.perform(
            delete("/api/v1/insights/" + insight.get("id").asLong()).header("Authorization", admin))
        .andExpect(status().isNoContent());
    mvc.perform(delete("/api/v1/contratos/" + contract).header("Authorization", admin))
        .andExpect(status().isNoContent());
    mvc.perform(delete("/api/v1/servicos/" + service).header("Authorization", admin))
        .andExpect(status().isNoContent());
  }

  @Test
  void paginaClientesFiltraPreservaContratoAnteriorEPersisteContato() throws Exception {
    String admin = auth(true);
    long consultant = consultor(admin);
    for (int i = 0; i < 3; i++) {
      var body = client(consultant, "PAG-" + UUID.randomUUID());
      body.put("email", "contato@example.com");
      create("clientes", body, admin);
    }
    mvc.perform(
            get("/api/v1/clientes/pagina")
                .param("pagina", "0")
                .param("tamanho", "2")
                .param("consultorId", Long.toString(consultant))
                .param("segmento", "serviços")
                .param("nivel", "A")
                .header("Authorization", admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("totalElements").value(3))
        .andExpect(jsonPath("content.length()").value(2))
        .andExpect(jsonPath("content[0].email").value("contato@example.com"));
    mvc.perform(get("/api/clientes").header("Authorization", admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
    mvc.perform(
            get("/api/v1/clientes/pagina").param("tamanho", "101").header("Authorization", admin))
        .andExpect(status().isBadRequest());
  }

  @Test
  void importacaoGravaEmailInsightEAuditoriaDepoisDoCommit() throws Exception {
    String admin = auth(true);
    String code = "IMP-" + UUID.randomUUID();
    Map<String, Object> row =
        new LinkedHashMap<>(
            Map.of(
                "codigo_cliente",
                code,
                "nome_cliente",
                "Empresa teste",
                "segmento",
                "Serviços",
                "nivel",
                "A",
                "email",
                "contato@example.com"));
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("records", List.of(row));
    data.put("columns", Map.of("clientCode", 0, "clientName", 1, "segment", 2));
    data.put("totalRows", 1);
    data.put("quality", 100.0);
    when(python.analyze(any(Path.class))).thenReturn(data);
    int before =
        database.queryForObject(
            "select count(*) from insight where tipo='IMPORTACAO'", Integer.class);
    mvc.perform(
            multipart("/api/v1/imports")
                .file(
                    new MockMultipartFile(
                        "file", "teste.xlsx", "application/octet-stream", new byte[] {1}))
                .header("Authorization", admin))
        .andExpect(status().isOk());
    assertEquals("contato@example.com", clientes.findByCodigoCti(code).orElseThrow().getEmail());
    assertEquals(
        before + 1,
        database.queryForObject(
            "select count(*) from insight where tipo='IMPORTACAO'", Integer.class));
    mvc.perform(get("/api/admin/telemetrias").header("Authorization", admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("content[0].tipoEvento").value("PROCESSAMENTO"))
        .andExpect(jsonPath("content[0].status").value("SUCESSO"));
  }

  @Test
  void falhaDaImportacaoReverteLoteMasPreservaAuditoriaSemDadosSensiveis() throws Exception {
    String admin = auth(true);
    when(python.analyze(any(Path.class)))
        .thenThrow(new IllegalStateException("SENHA_FICTICIA_NAO_PUBLICAR"));
    long before = database.queryForObject("select count(*) from import_batch", Long.class);
    var response =
        mvc.perform(
                multipart("/api/v1/imports")
                    .file(
                        new MockMultipartFile(
                            "file", "falha.xlsx", "application/octet-stream", new byte[] {1}))
                    .header("Authorization", admin))
            .andExpect(status().isInternalServerError())
            .andReturn();
    assertFalse(response.getResponse().getContentAsString().contains("SENHA_FICTICIA"));
    assertEquals(before, database.queryForObject("select count(*) from import_batch", Long.class));
    var audit =
        mvc.perform(get("/api/admin/telemetrias").header("Authorization", admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("content[0].status").value("ERRO"))
            .andReturn();
    assertFalse(audit.getResponse().getContentAsString().contains("SENHA_FICTICIA"));
  }

  @Test
  void migracaoAditivaPodeSerReaplicada() throws Exception {
    Path migration =
        Path.of("..", "deploy", "migrations", "V20261019_01__cadastros_comerciais.sql")
            .toAbsolutePath();
    try (var connection = database.getDataSource().getConnection()) {
      var resource = new org.springframework.core.io.FileSystemResource(migration);
      org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection, resource);
      org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection, resource);
    }
    assertEquals(
        0,
        database.queryForObject(
            "select count(*) from usuarios where status_cadastro is null", Integer.class));
  }

  @Autowired com.cti.provedor.repository.ImportBatchRepository batches;

  @Test
  void resultadoCientificoExigeAdminPreservaAnaliseEReenvioNaoDuplicaInsights() throws Exception {
    String admin = auth(true), user = auth(false);
    var batch = new com.cti.provedor.model.ImportBatch("cientifico.xlsx");
    batch.complete(1, 100, "{\"revenueStatistics\":{\"total\":123}}");
    batches.saveAndFlush(batch);
    var input =
        Map.of(
            "importacaoId",
            batch.getId(),
            "estatisticas",
            Map.of("media", 123),
            "insights",
            List.of("Insight científico de teste."));
    String json = mapper.writeValueAsString(input);
    mvc.perform(post("/api/analises/resultado").contentType("application/json").content(json))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/analises/resultado")
                .header("Authorization", user)
                .contentType("application/json")
                .content(json))
        .andExpect(status().isForbidden());
    for (int n = 0; n < 2; n++)
      mvc.perform(
              post("/api/analises/resultado")
                  .header("Authorization", admin)
                  .contentType("application/json")
                  .content(json))
          .andExpect(status().isOk());
    assertEquals(
        1,
        database.queryForObject(
            "select count(*) from insight where tipo='CIENCIA_DADOS_API' and descricao like ?",
            Integer.class,
            "Lote " + batch.getId() + ":%"));
    mvc.perform(get("/api/v1/imports/" + batch.getId()).header("Authorization", user))
        .andExpect(status().isOk())
        .andExpect(jsonPath("revenueStatistics.total").value(123))
        .andExpect(jsonPath("externalResult.statistics.media").value(123));
    mvc.perform(get("/api/v1/imports").header("Authorization", user))
        .andExpect(status().isOk())
        .andExpect(jsonPath("content").isArray());
    mvc.perform(get("/api/v1/imports?page=-1").header("Authorization", user))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/imports/" + UUID.randomUUID()).header("Authorization", user))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/analises/resultado")
                .header("Authorization", admin)
                .contentType("application/json")
                .content(
                    mapper.writeValueAsString(
                        Map.of(
                            "importacaoId",
                            batch.getId(),
                            "estatisticas",
                            Map.of("media", -1),
                            "insights",
                            List.of()))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void importacaoComercialCriaContratoPendenteComZeroSemDuplicar() throws Exception {
    String admin = auth(true);
    String code = "SCI-" + UUID.randomUUID();
    Map<String, Object> row =
        new LinkedHashMap<>(
            Map.of(
                "codigo_cliente",
                code,
                "cliente",
                "Empresa Ciência",
                "segmento",
                "Saúde",
                "nivel",
                "A",
                "consultor",
                "Consultor Ciência",
                "faturamento_mensal",
                "0",
                "servico",
                "Serviço Ciência",
                "valor_servico",
                "0",
                "status_contrato",
                "PENDENTE",
                "data_inicio",
                "2026-10-19"));
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("records", List.of(row));
    result.put("columns", Map.of("clientCode", 0, "segment", 2));
    result.put("totalRows", 1);
    result.put("quality", 100);
    result.put("analytics", Map.of("insights", List.of("Amostra comercial de teste.")));
    when(python.analyze(any(Path.class))).thenAnswer(call -> new LinkedHashMap<>(result));
    for (int n = 0; n < 2; n++)
      mvc.perform(
              multipart("/api/v1/imports")
                  .file(
                      new MockMultipartFile(
                          "file", "comercial.xlsx", "application/octet-stream", new byte[] {1}))
                  .header("Authorization", admin))
          .andExpect(status().isOk());
    assertEquals(
        1,
        database.queryForObject(
            "select count(*) from contrato c join cliente cl on cl.id_cliente=c.id_cliente where"
                + " cl.codigo_cti=? and c.status='PENDENTE' and c.valor=0",
            Integer.class,
            code));
    assertEquals(
        1,
        database.queryForObject(
            "select count(*) from cliente where codigo_cti=? and faturamento_anual=0",
            Integer.class,
            code));
  }
}
