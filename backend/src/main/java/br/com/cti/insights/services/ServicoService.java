package br.com.cti.insights.services;

import br.com.cti.insights.contracts.ContratoRepository;
import br.com.cti.insights.domain.Servico;
import br.com.cti.insights.shared.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ServicoService {
  private final ServicoRepository repository;
  private final ContratoRepository contratos;

  public ServicoService(ServicoRepository repository, ContratoRepository contratos) {
    this.repository = repository;
    this.contratos = contratos;
  }

  public PaginaResposta<ServicoResponse> list(int pagina, int tamanho) {
    return PaginaResposta.from(
        repository.findAll(Paginacao.criar(pagina, tamanho)).map(ServicoResponse::from));
  }

  public ServicoResponse get(Long id) {
    return ServicoResponse.from(find(id));
  }

  @Transactional
  public ServicoResponse create(ServicoRequest request) {
    return save(
        new Servico(request.nome().trim(), request.descricao(), request.categoria()), request);
  }

  @Transactional
  public ServicoResponse update(Long id, ServicoRequest request) {
    return save(find(id), request);
  }

  private ServicoResponse save(Servico entity, ServicoRequest request) {
    entity.update(request.nome().trim(), request.descricao(), request.categoria(), request.preco());
    return ServicoResponse.from(repository.saveAndFlush(entity));
  }

  @Transactional
  public void delete(Long id) {
    Servico entity = find(id);
    if (contratos.existsByServicoId(id))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Serviço possui contratos vinculados.");
    repository.delete(entity);
    repository.flush();
  }

  private Servico find(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço", id));
  }
}
