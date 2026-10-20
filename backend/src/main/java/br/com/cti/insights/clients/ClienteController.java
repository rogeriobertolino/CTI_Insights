package br.com.cti.insights.clients;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/clientes", "/api/v1/clientes"})
public class ClienteController {
  private final ClienteService service;

  public ClienteController(ClienteService service) {
    this.service = service;
  }

  @GetMapping
  public List<ClienteResponse> list() {
    return service.list();
  }

  @GetMapping("/pagina")
  public br.com.cti.insights.shared.PaginaResposta<ClienteResponse> pagina(
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "25") int tamanho,
      @RequestParam(required = false) Long consultorId,
      @RequestParam(required = false) String segmento,
      @RequestParam(required = false) br.com.cti.insights.domain.NivelCliente nivel) {
    return service.pagina(pagina, tamanho, consultorId, segmento, nivel);
  }

  @GetMapping("/{id}")
  public ClienteResponse get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public ResponseEntity<ClienteResponse> create(@Valid @RequestBody ClienteRequest request) {
    ClienteResponse response = service.create(request);
    return ResponseEntity.created(URI.create("/api/clientes/" + response.id())).body(response);
  }

  @PutMapping("/{id}")
  public ClienteResponse update(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
