package br.com.cti.insights.auth.dto;

public record LoginResponse(
    String token, String tipo, long expiraEmSegundos, UsuarioResponse usuario) {}
