package br.com.cti.insights.consultants;

import br.com.cti.insights.shared.PaginaResposta;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/consultores")
public class ConsultorController {
  private final ConsultorService service;

  public ConsultorController(ConsultorService service) {
    this.service = service;
  }

  @GetMapping
  public PaginaResposta<ConsultorResponse> list(
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "25") int tamanho) {
    return service.list(pagina, tamanho);
  }

  @GetMapping("/{id}")
  public ConsultorResponse get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public ResponseEntity<ConsultorResponse> create(@Valid @RequestBody ConsultorRequest request) {
    var response = service.create(request);
    return ResponseEntity.created(URI.create("/api/v1/consultores/" + response.id()))
        .body(response);
  }

  @PutMapping("/{id}")
  public ConsultorResponse update(
      @PathVariable Long id, @Valid @RequestBody ConsultorRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
