package com.cti.provedor.service;

import com.cti.provedor.dto.ClienteRequest;
import com.cti.provedor.dto.ClienteResponse;
import com.cti.provedor.dto.PaginaResposta;
import com.cti.provedor.model.Cliente;
import com.cti.provedor.model.Consultor;
import com.cti.provedor.model.NivelCliente;
import com.cti.provedor.model.StatusCliente;
import com.cti.provedor.repository.ClienteRepository;
import com.cti.provedor.repository.ConsultorRepository;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {
  private final ClienteRepository clientes;
  private final ConsultorRepository consultores;

  public ClienteService(ClienteRepository clientes, ConsultorRepository consultores) {
    this.clientes = clientes;
    this.consultores = consultores;
  }

  @Transactional(readOnly = true)
  public List<ClienteResponse> list() {
    return clientes.findAll().stream().map(ClienteResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public ClienteResponse get(Long id) {
    return ClienteResponse.from(find(id));
  }

  @Transactional
  public ClienteResponse create(ClienteRequest request) {
    if (clientes.existsByCodigoCti(request.codigoCti().trim())) {
      throw new ClienteDuplicadoException(request.codigoCti());
    }
    Cliente cliente =
        new Cliente(
            request.codigoCti().trim(),
            consultor(request.consultorId()),
            request.nomeEmpresa().trim(),
            request.segmento().trim(),
            request.faturamentoAnual(),
            request.nivel(),
            request.status() == null ? StatusCliente.ATIVO : request.status());
    cliente.setEmail(
        request.email() == null || request.email().isBlank()
            ? null
            : request.email().trim().toLowerCase(java.util.Locale.ROOT));
    return ClienteResponse.from(clientes.save(cliente));
  }

  @Transactional
  public ClienteResponse update(Long id, ClienteRequest request) {
    Cliente cliente = find(id);
    String codigo = request.codigoCti().trim();
    if (clientes.existsByCodigoCtiAndIdNot(codigo, id)) {
      throw new ClienteDuplicadoException(codigo);
    }
    cliente.update(
        codigo,
        consultor(request.consultorId()),
        request.nomeEmpresa().trim(),
        request.segmento().trim(),
        request.faturamentoAnual(),
        request.nivel(),
        request.status() == null ? StatusCliente.ATIVO : request.status());
    cliente.setEmail(
        request.email() == null || request.email().isBlank()
            ? null
            : request.email().trim().toLowerCase(java.util.Locale.ROOT));
    return ClienteResponse.from(clientes.save(cliente));
  }

  @Transactional
  public void delete(Long id) {
    var entity = find(id);
    clientes.delete(entity);
    clientes.flush();
  }

  @Transactional(readOnly = true)
  public PaginaResposta<ClienteResponse> pagina(
      int pagina, int tamanho, Long consultorId, String segmento, NivelCliente nivel) {
    if (consultorId != null && consultorId < 1)
      throw new IllegalArgumentException("ID do consultor inválido.");
    Specification<Cliente> spec =
        (root, query, cb) -> {
          var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
          if (consultorId != null)
            predicates.add(cb.equal(root.get("consultor").get("id"), consultorId));
          if (segmento != null && !segmento.isBlank())
            predicates.add(
                cb.equal(
                    cb.lower(root.get("segmento")),
                    segmento.trim().toLowerCase(java.util.Locale.ROOT)));
          if (nivel != null) predicates.add(cb.equal(root.get("nivel"), nivel));
          return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    return PaginaResposta.from(
        clientes.findAll(spec, Paginacao.criar(pagina, tamanho)).map(ClienteResponse::from));
  }

  private Cliente find(Long id) {
    return clientes
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));
  }

  private Consultor consultor(Long id) {
    return consultores
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Consultor", id));
  }
}
