package br.com.cti.insights.contracts;

import br.com.cti.insights.clients.ClienteRepository;
import br.com.cti.insights.domain.*;
import br.com.cti.insights.insights.InsightRepository;
import br.com.cti.insights.services.ServicoRepository;
import br.com.cti.insights.shared.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ContratoService {
  private final ContratoRepository repository;
  private final ClienteRepository clientes;
  private final ServicoRepository servicos;
  private final InsightRepository insights;

  public ContratoService(
      ContratoRepository repository,
      ClienteRepository clientes,
      ServicoRepository servicos,
      InsightRepository insights) {
    this.repository = repository;
    this.clientes = clientes;
    this.servicos = servicos;
    this.insights = insights;
  }

  public PaginaResposta<ContratoResponse> list(int pagina, int tamanho) {
    return PaginaResposta.from(
        repository.findAll(Paginacao.criar(pagina, tamanho)).map(ContratoResponse::from));
  }

  public ContratoResponse get(Long id) {
    return ContratoResponse.from(find(id));
  }

  @Transactional
  public ContratoResponse create(ContratoRequest request) {
    return save(new Contrato(), request);
  }

  @Transactional
  public ContratoResponse update(Long id, ContratoRequest request) {
    Contrato entity = find(id);
    if (!entity.getCliente().getId().equals(request.clienteId()) && insights.existsByContratoId(id))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Contrato possui insights; não é possível trocar seu cliente.");
    return save(entity, request);
  }

  private ContratoResponse save(Contrato entity, ContratoRequest request) {
    if (request.dataFim() != null && request.dataFim().isBefore(request.dataInicio()))
      throw new IllegalArgumentException("Data final não pode ser anterior à inicial.");
    var cliente =
        clientes
            .findById(request.clienteId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", request.clienteId()));
    var servico =
        servicos
            .findById(request.servicoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço", request.servicoId()));
    entity.update(
        cliente,
        servico,
        request.dataInicio(),
        request.dataFim(),
        request.valor(),
        request.status());
    return ContratoResponse.from(repository.saveAndFlush(entity));
  }

  @Transactional
  public void delete(Long id) {
    Contrato entity = find(id);
    if (insights.existsByContratoId(id))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Contrato possui insights vinculados.");
    repository.delete(entity);
    repository.flush();
  }

  private Contrato find(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Contrato", id));
  }
}
