package com.cti.provedor.dto;

import com.cti.provedor.model.StatusLote;
import com.cti.provedor.model.UploadLote;
import java.time.OffsetDateTime;

public record UploadLoteResponse(
    Long id,
    String arquivoNome,
    StatusLote status,
    int totalLinhas,
    int linhasValidas,
    int linhasInvalidas,
    int linhasComAviso,
    int linhasRejeitadasEtl,
    String mensagem,
    OffsetDateTime criadoEm,
    OffsetDateTime processadoEm) {
  public static UploadLoteResponse from(UploadLote lote) {
    return new UploadLoteResponse(
        lote.getId(), lote.getArquivoNome(), lote.getStatus(), lote.getTotalLinhas(),
        lote.getLinhasValidas(), lote.getLinhasInvalidas(), lote.getLinhasComAviso(),
        lote.getLinhasRejeitadasEtl(), lote.getMensagem(), lote.getCriadoEm(), lote.getProcessadoEm());
  }
}
