package br.com.cti.insights.telemetry;

import br.com.cti.insights.shared.PaginaResposta;
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
