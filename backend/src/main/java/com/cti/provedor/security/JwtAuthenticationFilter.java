package com.cti.provedor.security;

import com.cti.provedor.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

// Registrado somente na cadeia Spring Security, sem registro duplicado no container.
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwt;
  private final UsuarioRepository users;
  private final ObjectMapper mapper;

  public JwtAuthenticationFilter(JwtService jwt, UsuarioRepository users, ObjectMapper mapper) {
    this.jwt = jwt;
    this.users = users;
    this.mapper = mapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null) {
      try {
        if (!header.regionMatches(true, 0, "Bearer ", 0, 7))
          throw new MalformedJwtException("Use Bearer.");
        Claims claims = jwt.validarToken(header.substring(7));
        var user =
            users
                .findByEmail(claims.getSubject())
                .orElseThrow(() -> new MalformedJwtException("Usuário não encontrado."));
        if (!Objects.equals(user.getId(), claims.get("uid", Long.class)))
          throw new MalformedJwtException("Usuário inválido.");
        if (user.getStatusCadastro() != com.cti.provedor.model.StatusCadastro.APROVADO) {
          SecurityContextHolder.clearContext();
          response.setStatus(403);
          response.setContentType("application/json;charset=UTF-8");
          mapper.writeValue(
              response.getWriter(),
              Map.of(
                  "code",
                  "CADASTRO_NAO_APROVADO",
                  "message",
                  user.getStatusCadastro() == com.cti.provedor.model.StatusCadastro.PENDENTE
                      ? "Seu cadastro está aguardando aprovação do administrador."
                      : "Seu cadastro não foi autorizado. Entre em contato com o administrador."));
          return;
        }
        // Permissões atuais do banco prevalecem sobre um perfil antigo no token.
        var authentication =
            new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getPerfil().name())));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
      } catch (JwtException | IllegalArgumentException error) {
        SecurityContextHolder.clearContext();
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        mapper.writeValue(
            response.getWriter(),
            Map.of("message", "Token inválido ou expirado. Entre novamente."));
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
