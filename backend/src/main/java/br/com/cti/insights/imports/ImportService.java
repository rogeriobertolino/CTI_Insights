package br.com.cti.insights.imports;

import br.com.cti.insights.domain.*;
import br.com.cti.insights.telemetry.TelemetriaService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ImportService {
  private static final Logger LOG = LoggerFactory.getLogger(ImportService.class);
  private final ImportProcessingService processing;
  private final ImportBatchRepository batches;
  private final TelemetriaService telemetry;
  private final ObjectMapper mapper;

  public ImportService(
      ImportProcessingService processing,
      ImportBatchRepository batches,
      TelemetriaService telemetry,
      ObjectMapper mapper) {
    this.processing = processing;
    this.batches = batches;
    this.telemetry = telemetry;
    this.mapper = mapper;
  }

  public Map<String, Object> process(MultipartFile file) throws Exception {
    audit(file, TipoEventoTelemetria.UPLOAD, StatusTelemetria.SUCESSO);
    try {
      // A transação termina no serviço delegado antes de registrar o resultado.
      var result = processing.process(file);
      audit(file, TipoEventoTelemetria.PROCESSAMENTO, StatusTelemetria.SUCESSO);
      return result;
    } catch (Exception error) {
      audit(file, TipoEventoTelemetria.PROCESSAMENTO, StatusTelemetria.ERRO);
      throw error;
    }
  }

  @Transactional(readOnly = true)
  public Map<String, Object> latest() throws Exception {
    var batch =
        batches
            .findFirstByStatusOrderByCreatedAtDesc("COMPLETED")
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Nenhuma análise disponível."));
    Map<String, Object> result =
        mapper.readValue(batch.getAnalysisJson(), new TypeReference<>() {});
    return ImportProcessingService.response(batch, result);
  }

  private void audit(MultipartFile file, TipoEventoTelemetria tipo, StatusTelemetria status) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    String actor = authentication == null ? "sistema" : authentication.getName();
    try {
      telemetry.registrar(file.getOriginalFilename(), tipo, status, actor);
    } catch (RuntimeException error) {
      LOG.error("Falha ao registrar auditoria tipo={} classe={}", tipo, error.getClass().getName());
    }
  }
}
