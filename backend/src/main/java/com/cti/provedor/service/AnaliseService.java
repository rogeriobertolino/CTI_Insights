package com.cti.provedor.service;

import com.cti.provedor.dto.PaginaResposta;
import com.cti.provedor.dto.ResultadoAnaliseDTO;
import com.cti.provedor.model.ImportBatch;
import com.cti.provedor.model.Insight;
import com.cti.provedor.repository.ImportBatchRepository;
import com.cti.provedor.repository.InsightRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnaliseService {
  private final ImportBatchRepository batches;
  private final InsightRepository insights;
  private final ObjectMapper mapper;

  public AnaliseService(
      ImportBatchRepository batches, InsightRepository insights, ObjectMapper mapper) {
    this.batches = batches;
    this.insights = insights;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> find(UUID id) throws Exception {
    var batch = load(id);
    Map<String, Object> result =
        mapper.readValue(batch.getAnalysisJson(), new TypeReference<>() {});
    result.put("batchId", batch.getId());
    result.put("fileName", batch.getFileName());
    result.put("createdAt", batch.getCreatedAt());
    result.put("status", batch.getStatus());
    return result;
  }

  private ImportBatch load(UUID id) {
    return batches
        .findById(id)
        .filter(b -> "COMPLETED".equals(b.getStatus()))
        .orElseThrow(
            () ->
                new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Importação concluída não encontrada."));
  }

  @Transactional
  public Map<String, Object> save(ResultadoAnaliseDTO input) throws Exception {
    // Serializa atualizações concorrentes do mesmo lote e mantém o resultado original.
    var batch =
        batches
            .findForUpdate(input.importacaoId())
            .filter(b -> "COMPLETED".equals(b.getStatus()))
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Importação concluída não encontrada."));
    var result = find(input.importacaoId());
    result.remove("batchId");
    result.remove("fileName");
    result.remove("createdAt");
    result.remove("status");
    result.put(
        "externalResult", Map.of("statistics", input.estatisticas(), "insights", input.insights()));
    batch.complete(batch.getRowCount(), batch.getQualityScore(), mapper.writeValueAsString(result));
    String prefix = "Lote " + batch.getId() + ": ";
    insights.deleteAll(insights.findByTipoAndDescricaoStartingWith("CIENCIA_DADOS_API", prefix));
    for (String text : new LinkedHashSet<>(input.insights())) {
      var insight = new Insight();
      insight.update(null, null, "CIENCIA_DADOS_API", prefix + text);
      insights.save(insight);
    }
    return Map.of(
        "importacaoId", batch.getId(), "status", "SALVO", "insights", input.insights().size());
  }

  @Transactional(readOnly = true)
  public PaginaResposta<Map<String, Object>> history(int page, int size) {
    Paginacao.criar(page, size);
    return PaginaResposta.from(
        batches
            .findAll(
                org.springframework.data.domain.PageRequest.of(
                    page,
                    size,
                    org.springframework.data.domain.Sort.by("createdAt")
                        .descending()
                        .and(org.springframework.data.domain.Sort.by("id"))))
            .map(
                b -> {
                  Map<String, Object> row = new LinkedHashMap<>();
                  row.put("id", b.getId());
                  row.put("fileName", b.getFileName());
                  row.put("createdAt", b.getCreatedAt());
                  row.put("status", b.getStatus());
                  row.put("rowCount", b.getRowCount());
                  return row;
                }));
  }
}
