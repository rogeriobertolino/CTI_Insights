package com.cti.provedor.service;

import com.cti.provedor.dto.ClienteDTO;
import com.cti.provedor.repository.UploadLoteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * A ponte entre os dois backends: depois do lote salvo, envia as linhas válidas ao FastAPI
 * (POST /etl) com o cabeçalho X-ETL-Key. Java e Python ficam separados, cada um no seu processo.
 */
@Service
public class PythonEtlService {
  private static final Logger LOG = LoggerFactory.getLogger(PythonEtlService.class);
  static final String PYTHON_DESLIGADO =
      "Não foi possível falar com o serviço Python. Ligue o uvicorn (porta 8000) e envie a planilha de novo.";

  private final UploadLoteRepository lotes;
  private final ObjectMapper json;
  private final HttpClient http;
  private final URI etlUrl;
  private final URI saudeUrl;
  private final String chave;
  private final Duration timeout;

  public PythonEtlService(
      UploadLoteRepository lotes,
      ObjectMapper json,
      @Value("${cti.etl.url:http://localhost:8000/etl}") String url,
      @Value("${cti.etl.api-key:}") String chave,
      @Value("${cti.etl.timeout-seconds:120}") long timeoutSegundos) {
    this.lotes = lotes;
    this.json = json;
    this.etlUrl = URI.create(url);
    this.saudeUrl = etlUrl.resolve("saude");
    this.chave = chave == null ? "" : chave.trim();
    this.timeout = Duration.ofSeconds(timeoutSegundos);
    this.http =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
  }

  /** Roda depois do commit: se o banco desfez o lote, nada é enviado ao Python. */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void aoValidar(LoteValidadoEvent evento) {
    executar(evento);
  }

  public void executar(LoteValidadoEvent evento) {
    Long loteId = evento.loteId();
    if (!pythonLigado()) {
      marcarErro(loteId, PYTHON_DESLIGADO);
      return;
    }
    try {
      Map<String, Object> pedido = new LinkedHashMap<>();
      pedido.put("loteId", loteId);
      pedido.put("arquivoNome", evento.arquivoNome());
      pedido.put("recebidas", evento.recebidas());
      pedido.put("invalidas", evento.invalidas());
      pedido.put("comAviso", evento.comAviso());
      pedido.put("enviadoPor", evento.enviadoPor());
      pedido.put("linhas", linhas(evento.linhas()));

      var builder =
          HttpRequest.newBuilder(etlUrl)
              .timeout(timeout)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(pedido)));
      if (!chave.isEmpty()) builder.header("X-ETL-Key", chave);

      HttpResponse<String> resposta = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      if (resposta.statusCode() == 200) {
        LOG.info("Lote {} processado pelo Python.", loteId);
        return;
      }
      marcarErro(loteId, "O serviço Python respondeu " + resposta.statusCode() + ": " + detalhe(resposta.body()));
    } catch (HttpTimeoutException error) {
      marcarErro(loteId, "O serviço Python demorou mais de " + timeout.toSeconds() + " s para responder.");
    } catch (IOException error) {
      marcarErro(loteId, PYTHON_DESLIGADO);
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      marcarErro(loteId, "Envio ao serviço Python interrompido.");
    }
  }

  /** GET /saude: "você está ligado?" antes de mandar o trabalho. */
  boolean pythonLigado() {
    try {
      var pedido = HttpRequest.newBuilder(saudeUrl).timeout(Duration.ofSeconds(5)).GET().build();
      return http.send(pedido, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
    } catch (IOException error) {
      return false;
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private List<Map<String, Object>> linhas(List<ClienteDTO> validas) {
    return validas.stream()
        .map(
            l -> {
              Map<String, Object> linha = new LinkedHashMap<>();
              linha.put("linhaOrigem", l.linha());
              linha.put("codigoCliente", l.codigoCliente());
              linha.put("nomeCliente", l.nomeCliente());
              linha.put("consultor", l.consultor());
              linha.put("segmento", l.segmento());
              linha.put("nivelCliente", l.nivelCliente());
              linha.put("faturamentoAnual", l.faturamentoAnual());
              linha.put("servicosContratados", l.servicosContratados());
              linha.put("dataContratacao", l.dataContratacao());
              linha.put("cidade", l.cidade());
              linha.put("uf", l.uf());
              return linha;
            })
        .toList();
  }

  private String detalhe(String corpo) {
    try {
      var no = json.readTree(corpo).path("detail");
      if (no.isTextual()) return no.asText();
      if (!no.isMissingNode()) return no.toString();
    } catch (IOException ignored) {
      // corpo não é JSON: usa o texto como veio
    }
    return corpo == null || corpo.isBlank() ? "sem detalhes" : corpo;
  }

  private void marcarErro(Long loteId, String mensagem) {
    String texto = mensagem.length() > 500 ? mensagem.substring(0, 500) : mensagem;
    LOG.warn("Lote {} com ERRO: {}", loteId, texto);
    lotes.marcarErro(loteId, texto);
  }
}
