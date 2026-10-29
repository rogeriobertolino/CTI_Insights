package com.cti.provedor.controller;

import com.cti.provedor.dto.InsightRequest;
import com.cti.provedor.dto.InsightResponse;
import com.cti.provedor.dto.PaginaResposta;
import com.cti.provedor.service.InsightService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/insights")
public class InsightController {
  private final InsightService service;

  public InsightController(InsightService service) {
    this.service = service;
  }

  @GetMapping
  public PaginaResposta<InsightResponse> list(
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "25") int tamanho) {
    return service.list(pagina, tamanho);
  }

  @GetMapping("/{id}")
  public InsightResponse get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public ResponseEntity<InsightResponse> create(@Valid @RequestBody InsightRequest request) {
    var response = service.create(request);
    return ResponseEntity.created(URI.create("/api/v1/insights/" + response.id())).body(response);
  }

  @PutMapping("/{id}")
  public InsightResponse update(@PathVariable Long id, @Valid @RequestBody InsightRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
