package com.cti.provedor.controller;

import com.cti.provedor.dto.PaginaResposta;
import com.cti.provedor.dto.TelemetriaResponse;
import com.cti.provedor.service.TelemetriaService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/telemetrias")
public class TelemetriaController {
  private final TelemetriaService service;

  public TelemetriaController(TelemetriaService service) {
    this.service = service;
  }

  @GetMapping
  public PaginaResposta<TelemetriaResponse> list(
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "25") int tamanho) {
    return service.list(pagina, tamanho);
  }
}
