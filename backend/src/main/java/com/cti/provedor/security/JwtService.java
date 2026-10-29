package com.cti.provedor.security;

import com.cti.provedor.model.Usuario;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key;
  private final String issuer;
  private final long expirationSeconds;
  private final JwtParser parser;

  public JwtService(
      @Value("${cti.jwt.secret-base64}") String secret,
      @Value("${cti.jwt.issuer}") String issuer,
      @Value("${cti.jwt.expiration-seconds}") long expirationSeconds) {
    try {
      byte[] bytes = Decoders.BASE64.decode(secret);
      if (bytes.length < 32) throw new IllegalArgumentException("Chave muito curta");
      key = Keys.hmacShaKeyFor(bytes);
    } catch (RuntimeException error) {
      throw new IllegalArgumentException(
          "Configure JWT_SECRET_BASE64 com uma chave aleatória Base64 de pelo menos 32 bytes.");
    }
    if (issuer.isBlank() || expirationSeconds < 1)
      throw new IllegalArgumentException("Emissor e expiração JWT inválidos.");
    this.issuer = issuer;
    this.expirationSeconds = expirationSeconds;
    parser = Jwts.parser().verifyWith(key).requireIssuer(issuer).build();
  }

  public String gerarToken(Usuario user) {
    Instant now = Instant.now();
    return Jwts.builder()
        .issuer(issuer)
        .subject(user.getEmail())
        .claim("uid", user.getId())
        .claim("perfil", user.getPerfil().name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(expirationSeconds)))
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }

  public Claims validarToken(String token) {
    var signed = parser.parseSignedClaims(token);
    if (!"HS256".equals(signed.getHeader().getAlgorithm()))
      throw new MalformedJwtException("Algoritmo inválido.");
    Claims claims = signed.getPayload();
    if (claims.getSubject() == null
        || claims.getSubject().isBlank()
        || claims.getExpiration() == null
        || claims.get("uid", Long.class) == null)
      throw new MalformedJwtException("Token incompleto.");
    return claims;
  }

  public long expiracaoSegundos() {
    return expirationSeconds;
  }
}
