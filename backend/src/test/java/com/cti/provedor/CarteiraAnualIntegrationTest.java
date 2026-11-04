package com.cti.provedor;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties={"debug=false","logging.level.root=ERROR","spring.datasource.url=jdbc:h2:mem:annual;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="CTI_FULL_PIPELINE",matches="true")
class CarteiraAnualIntegrationTest {
  static Path root=Path.of("").toAbsolutePath().getParent();
  @DynamicPropertySource static void properties(DynamicPropertyRegistry p) {
    p.add("cti.python.mode",()->"native");
    p.add("cti.python.executable",()->System.getenv().getOrDefault("CTI_TEST_PYTHON",root.resolve(".venv/Scripts/python.exe").toString()));
    p.add("cti.python.script",()->root.resolve("python/analyze.py").toString());
  }
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @Test void amostraCompletaCria32Clientes61ContratosEReenvioNaoDuplica() throws Exception {
    var file=new MockMultipartFile("file","dados.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",Files.readAllBytes(root.resolve("sample-data/dados.xlsx")));
    for(int i=0;i<2;i++) {
      mvc.perform(multipart("/api/v1/imports").file(file).with(user("anual@example.com").roles("USER")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("analytics.inputRows").value(38))
          .andExpect(jsonPath("analytics.validRows").value(32))
          .andExpect(jsonPath("analytics.invalidRows").value(6))
          .andExpect(jsonPath("analytics.statistics.overall.mean").value(2261250))
          .andExpect(jsonPath("analytics.indicators.contractCount").value(61))
          .andExpect(jsonPath("analytics.invalidRecords").isEmpty());
    }
    assertEquals(32,db.queryForObject("select count(*) from cliente",Integer.class));
    assertEquals(4,db.queryForObject("select count(*) from consultor",Integer.class));
    assertEquals(6,db.queryForObject("select count(*) from servico",Integer.class));
    assertEquals(61,db.queryForObject("select count(*) from contrato where valor is null and status='PENDENTE'",Integer.class));
    assertEquals(32,db.queryForObject("select count(*) from cliente where cidade is not null and uf is not null",Integer.class));
    assertEquals(64,db.queryForObject("select count(*) from imported_record",Integer.class));
    assertEquals(2,db.queryForObject("select count(*) from import_batch",Integer.class));
  }
}
