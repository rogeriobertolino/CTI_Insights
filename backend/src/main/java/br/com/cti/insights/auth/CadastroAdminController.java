package br.com.cti.insights.auth;

import br.com.cti.insights.auth.dto.*;
import br.com.cti.insights.auth.service.CadastroAdminService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/cadastros")
public class CadastroAdminController {
  private final CadastroAdminService service;

  public CadastroAdminController(CadastroAdminService service) {
    this.service = service;
  }

  @GetMapping
  public CadastroAdminService.PaginaCadastros listar(
      @RequestParam(required = false) StatusCadastro status,
      @RequestParam(defaultValue = "0") int pagina) {
    return service.listar(status, pagina);
  }

  @PatchMapping("/{id}")
  public CadastroAdminResponse revisar(
      @PathVariable Long id,
      @Valid @RequestBody RevisaoCadastroRequest request,
      Authentication authentication) {
    return service.revisar(id, request, authentication.getName());
  }
}
