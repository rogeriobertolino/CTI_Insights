package br.com.cti.insights.services;

import br.com.cti.insights.shared.PaginaResposta;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/servicos")
public class ServicoController {
  private final ServicoService service;

  public ServicoController(ServicoService service) {
    this.service = service;
  }

  @GetMapping
  public PaginaResposta<ServicoResponse> list(
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "25") int tamanho) {
    return service.list(pagina, tamanho);
  }

  @GetMapping("/{id}")
  public ServicoResponse get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public ResponseEntity<ServicoResponse> create(@Valid @RequestBody ServicoRequest request) {
    var response = service.create(request);
    return ResponseEntity.created(URI.create("/api/v1/servicos/" + response.id())).body(response);
  }

  @PutMapping("/{id}")
  public ServicoResponse update(@PathVariable Long id, @Valid @RequestBody ServicoRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
