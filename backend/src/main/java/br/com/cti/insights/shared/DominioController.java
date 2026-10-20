package br.com.cti.insights.shared;

import br.com.cti.insights.domain.*;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dominios")
public class DominioController {
  @GetMapping
  public Map<String, Object> list() {
    return Map.of(
        "niveis",
        NivelCliente.values(),
        "statusClientes",
        StatusCliente.values(),
        "statusContratos",
        StatusContrato.values());
  }
}
