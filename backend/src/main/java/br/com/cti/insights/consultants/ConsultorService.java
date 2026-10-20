package br.com.cti.insights.consultants;

import br.com.cti.insights.clients.*;
import br.com.cti.insights.domain.Consultor;
import br.com.cti.insights.shared.*;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ConsultorService {
  private final ConsultorRepository repository;
  private final ClienteRepository clientes;

  public ConsultorService(ConsultorRepository repository, ClienteRepository clientes) {
    this.repository = repository;
    this.clientes = clientes;
  }

  public PaginaResposta<ConsultorResponse> list(int pagina, int tamanho) {
    return PaginaResposta.from(
        repository.findAll(Paginacao.criar(pagina, tamanho)).map(ConsultorResponse::from));
  }

  public ConsultorResponse get(Long id) {
    return ConsultorResponse.from(find(id));
  }

  @Transactional
  public ConsultorResponse create(ConsultorRequest request) {
    return save(
        new Consultor(
            request.nome().trim(),
            request.email().trim().toLowerCase(Locale.ROOT),
            request.telefone()),
        request);
  }

  @Transactional
  public ConsultorResponse update(Long id, ConsultorRequest request) {
    return save(find(id), request);
  }

  private ConsultorResponse save(Consultor entity, ConsultorRequest request) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);
    repository
        .findByEmailIgnoreCase(email)
        .filter(c -> !java.util.Objects.equals(c.getId(), entity.getId()))
        .ifPresent(
            c -> {
              throw new ResponseStatusException(
                  HttpStatus.CONFLICT, "E-mail de consultor já cadastrado.");
            });
    entity.update(request.nome().trim(), email, request.telefone(), request.matricula());
    return ConsultorResponse.from(repository.saveAndFlush(entity));
  }

  @Transactional
  public void delete(Long id) {
    Consultor entity = find(id);
    if (clientes.existsByConsultorId(id))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Consultor possui clientes vinculados.");
    repository.delete(entity);
    repository.flush();
  }

  private Consultor find(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Consultor", id));
  }
}
