package com.cti.provedor.service;

import com.cti.provedor.dto.UploadLoteResponse;
import com.cti.provedor.dto.UploadRequest;
import com.cti.provedor.dto.UploadResultado.LinhaRejeitada;
import com.cti.provedor.model.StatusLote;
import com.cti.provedor.model.UploadLote;
import com.cti.provedor.repository.EstatisticaLoteRepository;
import com.cti.provedor.repository.UploadLoteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Coordena o upload: separa linhas aprovadas e rejeitadas, grava o lote como PENDENTE e publica o
 * evento que leva as aprovadas ao Python. O Spring não grava clientes: isso é trabalho do ETL.
 */
@Service
public class UploadService {
  private final ClienteValidacaoService validacao;
  private final UploadLoteRepository lotes;
  private final EstatisticaLoteRepository estatisticas;
  private final ApplicationEventPublisher eventos;
  private final ObjectMapper mapper;

  // O status do lote muda fora desta requisição (Python e marcarErro): sempre reler do banco.
  @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

  public UploadService(
      ClienteValidacaoService validacao,
      UploadLoteRepository lotes,
      EstatisticaLoteRepository estatisticas,
      ApplicationEventPublisher eventos,
      ObjectMapper mapper) {
    this.validacao = validacao;
    this.lotes = lotes;
    this.estatisticas = estatisticas;
    this.eventos = eventos;
    this.mapper = mapper;
  }

  public record Recebimento(Long loteId, List<LinhaRejeitada> rejeitadas) {}

  @Transactional
  public Recebimento receber(UploadRequest pedido, String usuario) {
    if (pedido.recebidas() != pedido.linhas().size() + pedido.invalidas())
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Contagens inconsistentes: recebidas deve ser igual às linhas enviadas mais as inválidas.");
    if (pedido.comAviso() > pedido.linhas().size())
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Contagens inconsistentes: há mais avisos do que linhas enviadas.");

    var resultado = validacao.validar(pedido.linhas());
    if (resultado.validas().isEmpty())
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Nenhuma linha passou na validação do servidor. Corrija a planilha e envie de novo.");

    int invalidas = pedido.invalidas() + resultado.rejeitadas().size();
    var lote =
        lotes.save(
            new UploadLote(
                pedido.arquivoNome().trim(),
                pedido.recebidas(),
                resultado.validas().size(),
                invalidas,
                Math.min(pedido.comAviso(), resultado.validas().size()),
                usuario));
    // O PythonEtlService escuta este evento e só roda depois do COMMIT.
    eventos.publishEvent(
        new LoteValidadoEvent(
            lote.getId(),
            lote.getArquivoNome(),
            pedido.recebidas(),
            invalidas,
            lote.getLinhasComAviso(),
            usuario,
            resultado.validas()));
    return new Recebimento(lote.getId(), resultado.rejeitadas());
  }

  @Transactional(readOnly = true)
  public UploadLoteResponse consultar(Long id) {
    return UploadLoteResponse.from(buscar(id));
  }

  @Transactional(readOnly = true)
  public List<UploadLoteResponse> listar() {
    return lotes.findTop50ByOrderByIdDesc().stream().map(UploadLoteResponse::from).toList();
  }

  /** Lê o JSON que o Python salvou em estatistica_lote. Sem loteId: o último PROCESSADO. */
  @Transactional(readOnly = true)
  public Map<String, Object> dashboard(Long loteId) {
    UploadLote lote =
        loteId == null
            ? lotes
                .findFirstByStatusOrderByIdDesc(StatusLote.PROCESSADO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum lote processado ainda."))
            : buscar(loteId);
    if (lote.getStatus() != StatusLote.PROCESSADO)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "O lote " + lote.getId() + " está " + lote.getStatus()
              + (lote.getMensagem() == null ? "." : ": " + lote.getMensagem()));
    var estatistica =
        estatisticas
            .findByLoteId(lote.getId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "O lote " + lote.getId() + " não tem análises salvas."));
    JsonNode json;
    try {
      json = mapper.readTree(estatistica.getResultado());
    } catch (Exception error) {
      throw new IllegalStateException("JSON de estatísticas inválido no lote " + lote.getId(), error);
    }
    Map<String, Object> resposta = new LinkedHashMap<>();
    resposta.put("lote", UploadLoteResponse.from(lote));
    resposta.put("estatisticas", json);
    return resposta;
  }

  private UploadLote buscar(Long id) {
    UploadLote lote = lotes.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Lote", id));
    entityManager.refresh(lote);
    return lote;
  }
}
