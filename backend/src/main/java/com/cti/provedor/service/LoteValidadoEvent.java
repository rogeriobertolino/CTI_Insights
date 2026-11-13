package com.cti.provedor.service;

import com.cti.provedor.dto.ClienteDTO;
import java.util.List;

/** Publicado pelo UploadService; o PythonEtlService só age depois do commit do lote. */
public record LoteValidadoEvent(
    Long loteId,
    String arquivoNome,
    int recebidas,
    int invalidas,
    int comAviso,
    String enviadoPor,
    List<ClienteDTO> linhas) {}
