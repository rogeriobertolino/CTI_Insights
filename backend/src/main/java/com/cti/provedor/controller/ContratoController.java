package com.cti.provedor.controller;

import com.cti.provedor.dto.ContratoRequest;
import com.cti.provedor.dto.ContratoResponse;
import com.cti.provedor.dto.PaginaResposta;
import com.cti.provedor.service.ContratoService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contratos")
public class ContratoController {
  private final ContratoService service;

  public ContratoController(ContratoService service) {
    this.service = service;
  }

  @GetMapping
  public PaginaResposta<ContratoResponse> list(
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "25") int tamanho) {
    return service.list(pagina, tamanho);
  }

  @GetMapping("/{id}")
  public ContratoResponse get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public ResponseEntity<ContratoResponse> create(@Valid @RequestBody ContratoRequest request) {
    var response = service.create(request);
    return ResponseEntity.created(URI.create("/api/v1/contratos/" + response.id())).body(response);
  }

  @PutMapping("/{id}")
  public ContratoResponse update(
      @PathVariable Long id, @Valid @RequestBody ContratoRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
