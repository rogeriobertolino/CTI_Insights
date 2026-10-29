package com.cti.provedor.config;

import com.cti.provedor.security.JwtService;
import com.cti.provedor.security.JwtAuthenticationFilter;

import com.cti.provedor.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwords() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  org.springframework.web.cors.CorsConfigurationSource corsSource(
      @org.springframework.beans.factory.annotation.Value(
              "${cti.cors.origin:http://localhost:5173}")
          String origins) {
    var config = new org.springframework.web.cors.CorsConfiguration();
    config.setAllowedOrigins(
        Arrays.stream(origins.split(",")).map(String::trim).filter(v -> !v.isBlank()).toList());
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setExposedHeaders(List.of("Location"));
    config.setAllowCredentials(false);
    config.setMaxAge(3600L);
    var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      ObjectMapper mapper,
      JwtService jwt,
      UsuarioRepository users,
      org.springframework.web.cors.CorsConfigurationSource corsSource)
      throws Exception {
    // Apenas Bearer; cookies não estabelecem autenticação nesta API.
    http.cors(cors -> cors.configurationSource(corsSource))
        .csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.authorizeHttpRequests(
        auth ->
            auth.requestMatchers(
                    "/api/auth/cadastro",
                    "/api/auth/register",
                    "/api/auth/login",
                    "/actuator/health",
                    "/error")
                .permitAll()
                .requestMatchers("/api/admin", "/api/admin/**")
                .hasRole("ADMIN")
                .requestMatchers("/api/admin/telemetrias", "/api/admin/telemetrias/**")
                .hasRole("ADMIN")
                .requestMatchers(
                    org.springframework.http.HttpMethod.POST,
                    "/api/analises/resultado",
                    "/api/v1/consultores/**",
                    "/api/v1/servicos/**",
                    "/api/v1/contratos/**",
                    "/api/v1/insights/**")
                .hasRole("ADMIN")
                .requestMatchers(
                    org.springframework.http.HttpMethod.PUT,
                    "/api/v1/consultores/**",
                    "/api/v1/servicos/**",
                    "/api/v1/contratos/**",
                    "/api/v1/insights/**")
                .hasRole("ADMIN")
                .requestMatchers(
                    org.springframework.http.HttpMethod.PATCH,
                    "/api/v1/consultores/**",
                    "/api/v1/servicos/**",
                    "/api/v1/contratos/**",
                    "/api/v1/insights/**")
                .hasRole("ADMIN")
                .requestMatchers(
                    org.springframework.http.HttpMethod.DELETE,
                    "/api/v1/consultores/**",
                    "/api/v1/servicos/**",
                    "/api/v1/contratos/**",
                    "/api/v1/insights/**")
                .hasRole("ADMIN")
                .requestMatchers("/api/**")
                .hasAnyRole("USER", "ADMIN")
                .anyRequest()
                .permitAll());
    http.addFilterBefore(
        new JwtAuthenticationFilter(jwt, users, mapper),
        UsernamePasswordAuthenticationFilter.class);
    http.exceptionHandling(
        errors ->
            errors
                .authenticationEntryPoint(
                    (request, response, error) -> {
                      response.setStatus(401);
                      response.setContentType("application/json;charset=UTF-8");
                      mapper.writeValue(
                          response.getWriter(),
                          Map.of("message", "Entre na sua conta para continuar."));
                    })
                .accessDeniedHandler(
                    (request, response, error) -> {
                      response.setStatus(403);
                      response.setContentType("application/json;charset=UTF-8");
                      mapper.writeValue(
                          response.getWriter(),
                          Map.of("message", "Seu perfil não permite acessar este recurso."));
                    }));
    return http.build();
  }
}
