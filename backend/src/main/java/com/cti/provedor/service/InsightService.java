package com.cti.provedor.service;

import com.cti.provedor.dto.InsightRequest;
import com.cti.provedor.dto.InsightResponse;
import com.cti.provedor.dto.PaginaResposta;
import com.cti.provedor.model.Insight;
import com.cti.provedor.repository.ClienteRepository;
import com.cti.provedor.repository.ContratoRepository;
import com.cti.provedor.repository.InsightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InsightService {
  private final InsightRepository repository;
  private final ClienteRepository clientes;
  private final ContratoRepository contratos;

  public InsightService(
      InsightRepository repository, ClienteRepository clientes, ContratoRepository contratos) {
    this.repository = repository;
    this.clientes = clientes;
    this.contratos = contratos;
  }

  public PaginaResposta<InsightResponse> list(int pagina, int tamanho) {
    return PaginaResposta.from(
        repository.findAll(Paginacao.criar(pagina, tamanho)).map(InsightResponse::from));
  }

  public InsightResponse get(Long id) {
    return InsightResponse.from(find(id));
  }

  @Transactional
  public InsightResponse create(InsightRequest request) {
    return save(new Insight(), request);
  }

  @Transactional
  public InsightResponse update(Long id, InsightRequest request) {
    return save(find(id), request);
  }

  private InsightResponse save(Insight entity, InsightRequest request) {
    var contrato =
        request.contratoId() == null
            ? null
            : contratos
                .findById(request.contratoId())
                .orElseThrow(
                    () -> new RecursoNaoEncontradoException("Contrato", request.contratoId()));
    var cliente =
        request.clienteId() == null
            ? null
            : clientes
                .findById(request.clienteId())
                .orElseThrow(
                    () -> new RecursoNaoEncontradoException("Cliente", request.clienteId()));
    if (contrato != null) {
      if (cliente != null && !contrato.getCliente().getId().equals(cliente.getId()))
        throw new IllegalArgumentException("O contrato não pertence ao cliente informado.");
      cliente = contrato.getCliente();
    }
    entity.update(cliente, contrato, request.tipo().trim(), request.descricao().trim());
    return InsightResponse.from(repository.saveAndFlush(entity));
  }

  @Transactional
  public void delete(Long id) {
    repository.delete(find(id));
    repository.flush();
  }

  private Insight find(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Insight", id));
  }
}
