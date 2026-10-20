package br.com.cti.insights.imports;

import br.com.cti.insights.domain.Insight;
import br.com.cti.insights.insights.InsightRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportProcessingService {
  private final ImportValidationService validation;
  private final ImportBatchRepository batches;
  private final ImportedRecordRepository records;
  private final PythonAnalysisService python;
  private final ImportDomainService domain;
  private final ObjectMapper mapper;
  private final InsightRepository insights;

  public ImportProcessingService(
      ImportValidationService validation,
      ImportBatchRepository batches,
      ImportedRecordRepository records,
      PythonAnalysisService python,
      ImportDomainService domain,
      ObjectMapper mapper,
      InsightRepository insights) {
    this.validation = validation;
    this.batches = batches;
    this.records = records;
    this.python = python;
    this.domain = domain;
    this.mapper = mapper;
    this.insights = insights;
  }

  @Transactional(rollbackFor = Exception.class)
  public Map<String, Object> process(MultipartFile file) throws Exception {
    String name = file.getOriginalFilename();
    if (file.isEmpty() || name == null || !name.toLowerCase(Locale.ROOT).endsWith(".xlsx"))
      throw new ExcelInvalidoException("Envie um arquivo XLSX válido.");
    if (name.length() > 255)
      throw new ExcelInvalidoException("Nome do arquivo deve ter até 255 caracteres.");
    ImportBatch batch = batches.save(new ImportBatch(name));
    Path temp = Files.createTempFile("cti-", ".xlsx");
    try {
      file.transferTo(temp);
      Map<String, Object> result = python.analyze(temp);
      validateColumns(result);
      List<Map<String, Object>> imported =
          (List<Map<String, Object>>) result.getOrDefault("records", List.of());
      validation.validate(imported);
      List<ImportedRecord> entities = new ArrayList<>();
      for (int i = 0; i < imported.size(); i++) {
        var row = imported.get(i);
        entities.add(
            new ImportedRecord(
                batch.getId(), sourceLine(row, i + 2), mapper.writeValueAsString(row)));
      }
      records.saveAll(entities);
      int created = domain.persistClients(imported);
      result.put("records", imported.subList(0, Math.min(imported.size(), 1000)));
      long rows = ((Number) result.getOrDefault("totalRows", 0)).longValue();
      double quality = ((Number) result.getOrDefault("quality", 0)).doubleValue();
      batch.complete(rows, quality, mapper.writeValueAsString(result));
      batches.save(batch);
      Insight insight = new Insight();
      insight.update(
          null,
          null,
          "IMPORTACAO",
          "Lote "
              + batch.getId()
              + ": "
              + rows
              + " registros processados e "
              + created
              + " clientes criados.");
      insights.save(insight);
      Object analyticsValue = result.get("analytics");
      if (analyticsValue instanceof Map<?, ?> analytics
          && analytics.get("insights") instanceof List<?> texts) {
        for (Object text : texts) {
          var generated = new Insight();
          generated.update(null, null, "CIENCIA_DADOS", "Lote " + batch.getId() + ": " + text);
          insights.save(generated);
        }
      }
      return response(batch, result);
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  static Map<String, Object> response(ImportBatch batch, Map<String, Object> result) {
    Map<String, Object> response = new LinkedHashMap<>(result);
    response.put("batchId", batch.getId());
    response.put("fileName", batch.getFileName());
    response.put("status", batch.getStatus());
    response.put("createdAt", batch.getCreatedAt());
    return response;
  }

  private void validateColumns(Map<String, Object> result) {
    Map<String, Object> columns = (Map<String, Object>) result.getOrDefault("columns", Map.of());
    if (number(columns.get("clientCode")) < 0 && number(columns.get("clientName")) < 0)
      throw new ColunaObrigatoriaException("cliente ou código do cliente");
    if (number(columns.get("segment")) < 0) throw new ColunaObrigatoriaException("segmento");
  }

  private int sourceLine(Map<String, Object> row, int fallback) {
    try {
      return Integer.parseInt(row.getOrDefault("linha_origem", fallback).toString());
    } catch (NumberFormatException error) {
      return fallback;
    }
  }

  private int number(Object value) {
    return value instanceof Number n ? n.intValue() : -1;
  }
}
