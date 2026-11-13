package com.cti.provedor.controller;

import com.cti.provedor.dto.UploadLoteResponse;
import com.cti.provedor.dto.UploadRequest;
import com.cti.provedor.dto.UploadResultado;
import com.cti.provedor.model.StatusLote;
import com.cti.provedor.model.StatusTelemetria;
import com.cti.provedor.model.TipoEventoTelemetria;
import com.cti.provedor.service.TelemetriaService;
import com.cti.provedor.service.UploadService;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Recebe o JSON das linhas validadas pelo Vue e o encaminha ao UploadService. */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {
  private static final Logger LOG = LoggerFactory.getLogger(UploadController.class);
  private final UploadService service;
  private final TelemetriaService telemetria;

  public UploadController(UploadService service, TelemetriaService telemetria) {
    this.service = service;
    this.telemetria = telemetria;
  }

  @PostMapping("/validadas")
  public ResponseEntity<UploadResultado> validadas(
      @Valid @RequestBody UploadRequest pedido, Authentication autenticacao) {
    String usuario = autenticacao.getName();
    auditar(pedido.arquivoNome(), TipoEventoTelemetria.UPLOAD, StatusTelemetria.SUCESSO, usuario);
    // Quando receber() retorna, o lote já foi salvo e o Python já respondeu (ou falhou).
    var recebimento = service.receber(pedido, usuario);
    UploadLoteResponse lote = service.consultar(recebimento.loteId());
    auditar(
        pedido.arquivoNome(),
        TipoEventoTelemetria.PROCESSAMENTO,
        lote.status() == StatusLote.PROCESSADO ? StatusTelemetria.SUCESSO : StatusTelemetria.ERRO,
        usuario);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new UploadResultado(lote, recebimento.rejeitadas()));
  }

  @GetMapping
  public List<UploadLoteResponse> listar() {
    return service.listar();
  }

  @GetMapping("/{id}")
  public UploadLoteResponse consultar(@PathVariable Long id) {
    return service.consultar(id);
  }

  private void auditar(String arquivo, TipoEventoTelemetria tipo, StatusTelemetria status, String usuario) {
    try {
      telemetria.registrar(arquivo, tipo, status, usuario);
    } catch (RuntimeException error) {
      LOG.error("Falha ao registrar auditoria tipo={} classe={}", tipo, error.getClass().getName());
    }
  }
}
