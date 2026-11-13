package com.cti.provedor.dto;

import java.util.List;

/** Resposta do upload: o lote e as linhas que o backend recusou (com o motivo). */
public record UploadResultado(UploadLoteResponse lote, List<LinhaRejeitada> rejeitadas) {
  public record LinhaRejeitada(Integer linha, String codigoCliente, List<String> motivos) {}
}
