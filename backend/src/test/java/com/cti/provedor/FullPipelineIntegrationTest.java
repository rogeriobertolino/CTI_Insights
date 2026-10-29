package com.cti.provedor;


import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "debug=false",
      "logging.level.root=WARN",
      "spring.datasource.url=jdbc:h2:mem:fullpipeline;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "CTI_FULL_PIPELINE", matches = "true")
class FullPipelineIntegrationTest {
  static Path root = Path.of("").toAbsolutePath().getParent();
  static Path python =
      System.getenv("CTI_TEST_PYTHON") != null
          ? Path.of(System.getenv("CTI_TEST_PYTHON"))
          : root.resolve(
              System.getProperty("os.name").toLowerCase().contains("win")
                  ? ".venv/Scripts/python.exe"
                  : ".venv/bin/python");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry properties) {
    properties.add(
        "cti.python.mode", () -> System.getenv().getOrDefault("CTI_TEST_PYTHON_MODE", "native"));
    properties.add("cti.python.executable", () -> python.toString());
    properties.add("cti.python.script", () -> root.resolve("python/analyze.py").toString());
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate database;
  @Autowired com.cti.provedor.repository.UsuarioRepository users;
  @Autowired com.cti.provedor.security.JwtService jwt;
  @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;

  @Test
  void pythonRealImportacaoPersistenciaDashboardERollback() throws Exception {
    var user =
        users.saveAndFlush(
            new com.cti.provedor.model.Usuario(
                "Teste Pipeline", "pipeline@example.com", passwords.encode("senha-teste-123")));
    String authToken = jwt.gerarToken(user);
    Path file = Files.createTempFile("cti-pipeline-test-", ".xlsx");
    Path log = Files.createTempFile("cti-pipeline-test-", ".log");
    Process process = null;
    try {
      String script =
          "from openpyxl import Workbook;import"
              + " sys;w=Workbook();s=w.active;s.append(['codigo_cliente','nome_cliente','segmento','nivel','faturamento_mensal','linha_origem','consultor','servico','valor_servico','status_contrato','data_inicio']);s.append(['PIPELINE-TEST','Empresa"
              + " 001','Serviços','A',1234.56,7,'Consultor Pipeline','Internet"
              + " Pipeline',0,'PENDENTE','2026-10-19']);w.save(sys.argv[1]);w.close()";
      process =
          new ProcessBuilder(python.toString(), "-c", script, file.toString())
              .redirectErrorStream(true)
              .redirectOutput(log.toFile())
              .start();
      if (!process.waitFor(120, TimeUnit.SECONDS)) {
        stopProcess(process);
        fail("Python fixture timeout: " + Files.readString(log));
      }
      assertEquals(0, process.exitValue(), Files.readString(log));
      MockMultipartFile upload =
          new MockMultipartFile(
              "file",
              "validado.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              Files.readAllBytes(file));
      mvc.perform(
              multipart("/api/v1/imports")
                  .file(upload)
                  .header("Authorization", "Bearer " + authToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("totalRows").value(1))
          .andExpect(jsonPath("revenueStatistics.total").value(1234.56));
      mvc.perform(get("/api/v1/dashboard/latest").header("Authorization", "Bearer " + authToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("records[0].cliente").value("Empresa 001"));
      assertEquals(
          14814.72,
          database.queryForObject(
              "select faturamento_anual from cliente where codigo_cti='PIPELINE-TEST'",
              Double.class),
          0.001);
      assertEquals(
          1,
          database.queryForObject(
              "select count(*) from contrato where status='PENDENTE' and valor=0", Integer.class));
      assertEquals(
          7, database.queryForObject("select row_number from imported_record", Integer.class));
      String invalid = script.replace("1234.56", "-1").replace("PIPELINE-TEST", "PIPELINE-INVALID");
      process =
          new ProcessBuilder(python.toString(), "-c", invalid, file.toString())
              .redirectErrorStream(true)
              .redirectOutput(log.toFile())
              .start();
      if (!process.waitFor(120, TimeUnit.SECONDS)) {
        stopProcess(process);
        fail("Python fixture timeout: " + Files.readString(log));
      }
      assertEquals(0, process.exitValue(), Files.readString(log));
      upload =
          new MockMultipartFile(
              "file",
              "invalido.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              Files.readAllBytes(file));
      mvc.perform(
              multipart("/api/v1/imports")
                  .file(upload)
                  .header("Authorization", "Bearer " + authToken))
          .andExpect(status().isBadRequest());
      assertEquals(1, database.queryForObject("select count(*) from import_batch", Integer.class));
      assertEquals(
          0,
          database.queryForObject(
              "select count(*) from cliente where codigo_cti='PIPELINE-INVALID'", Integer.class));
    } finally {
      if (process != null && process.isAlive()) stopProcess(process);
      Files.deleteIfExists(file);
      Files.deleteIfExists(log);
    }
  }

  private static void stopProcess(Process process) {
    var children = process.descendants().toList();
    children.forEach(ProcessHandle::destroyForcibly);
    process.destroyForcibly();
    process.onExit().join();
    children.forEach(child -> child.onExit().join());
  }
}
