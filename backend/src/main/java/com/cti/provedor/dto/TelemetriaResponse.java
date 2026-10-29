package com.cti.provedor.dto;

import com.cti.provedor.model.StatusTelemetria;
import com.cti.provedor.model.Telemetria;
import com.cti.provedor.model.TipoEventoTelemetria;
import java.time.LocalDateTime;

public record TelemetriaResponse(
    Long id,
    String arquivoNome,
    LocalDateTime dataHora,
    TipoEventoTelemetria tipoEvento,
    StatusTelemetria status,
    String mensagemErro,
    String usuario) {
  public static TelemetriaResponse from(Telemetria t) {
    return new TelemetriaResponse(
        t.getId(),
        t.getArquivoNome(),
        t.getDataHora(),
        t.getTipoEvento(),
        t.getStatus(),
        t.getMensagemErro(),
        t.getUsuario());
  }
}
