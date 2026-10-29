package com.cti.provedor.controller;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class PrivadoController {
  @GetMapping("/api/privado")
  public Map<String, String> privado(Authentication authentication) {
    return Map.of("message", "Acesso autenticado autorizado.", "email", authentication.getName());
  }

  @GetMapping("/api/admin")
  public Map<String, String> admin() {
    return Map.of("message", "Acesso administrativo autorizado.");
  }
}
