package com.cti.provedor.service;

import com.cti.provedor.dto.PaginaResposta;
import com.cti.provedor.dto.TelemetriaResponse;
import com.cti.provedor.model.StatusTelemetria;
import com.cti.provedor.model.Telemetria;
import com.cti.provedor.model.TipoEventoTelemetria;
import com.cti.provedor.repository.TelemetriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class TelemetriaService {
  private final TelemetriaRepository repository;

  public TelemetriaService(TelemetriaRepository repository) {
    this.repository = repository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void registrar(
      String arquivo, TipoEventoTelemetria tipo, StatusTelemetria status, String usuario) {
    String nome = arquivo == null ? "arquivo" : arquivo.replace('\\', '/');
    nome = nome.substring(nome.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
    if (nome.isBlank()) nome = "arquivo";
    if (nome.length() > 255) nome = nome.substring(0, 255);
    repository.saveAndFlush(
        new Telemetria(
            nome,
            tipo,
            status,
            status == StatusTelemetria.ERRO ? "Não foi possível concluir a importação." : null,
            usuario));
  }

  @Transactional(readOnly = true)
  public PaginaResposta<TelemetriaResponse> list(int pagina, int tamanho) {
    return PaginaResposta.from(
        repository.findAll(Paginacao.criar(pagina, tamanho)).map(TelemetriaResponse::from));
  }
}
