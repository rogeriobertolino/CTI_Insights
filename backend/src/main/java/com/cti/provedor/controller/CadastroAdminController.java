package com.cti.provedor.controller;

import com.cti.provedor.dto.CadastroAdminResponse;
import com.cti.provedor.dto.RevisaoCadastroRequest;
import com.cti.provedor.model.StatusCadastro;
import com.cti.provedor.service.CadastroAdminService;
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
