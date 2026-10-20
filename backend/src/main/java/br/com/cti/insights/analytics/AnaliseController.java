package br.com.cti.insights.analytics;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class AnaliseController {
  private final AnaliseService service;

  public AnaliseController(AnaliseService service) {
    this.service = service;
  }

  @PostMapping("/api/analises/resultado")
  public Map<String, Object> save(@Valid @RequestBody ResultadoAnaliseDTO body) throws Exception {
    return service.save(body);
  }

  @GetMapping("/api/v1/imports")
  public br.com.cti.insights.shared.PaginaResposta<Map<String, Object>> history(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
    return service.history(page, size);
  }

  @GetMapping("/api/v1/imports/{id}")
  public Map<String, Object> find(@PathVariable UUID id) throws Exception {
    return service.find(id);
  }
}
