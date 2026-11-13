package com.cti.provedor.controller;

import com.cti.provedor.dto.AlterarSenhaRequest;
import com.cti.provedor.dto.CadastroRequest;
import com.cti.provedor.dto.LoginRequest;
import com.cti.provedor.dto.LoginResponse;
import com.cti.provedor.dto.RedefinirSenhaRequest;
import com.cti.provedor.dto.SolicitarRedefinicaoSenhaRequest;
import com.cti.provedor.dto.UsuarioResponse;
import com.cti.provedor.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService service;

  public AuthController(AuthService service) {
    this.service = service;
  }

  @PostMapping({"/cadastro", "/register"})
  public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody CadastroRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrar(request));
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return service.login(request);
  }

  @PostMapping("/recuperar-senha")
  public ResponseEntity<java.util.Map<String, String>> solicitarRedefinicao(
      @Valid @RequestBody SolicitarRedefinicaoSenhaRequest request) {
    return ResponseEntity.accepted().body(java.util.Map.of(
        "message", service.solicitarRedefinicaoSenha(request)));
  }

  @PostMapping("/redefinir-senha")
  public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
    service.redefinirSenha(request);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/alterar-senha")
  public ResponseEntity<Void> alterarSenha(
      Authentication authentication, @Valid @RequestBody AlterarSenhaRequest request) {
    service.alterarSenha(authentication.getName(), request);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  public UsuarioResponse me(Authentication authentication) {
    return service.usuario(authentication.getName());
  }

  // O cliente descarta o JWT. Tokens emitidos continuam sujeitos à expiração.
  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {
    return ResponseEntity.noContent().build();
  }
}
