package com.cti.provedor.service;

import com.cti.provedor.dto.ClienteDTO;
import com.cti.provedor.dto.UploadResultado.LinhaRejeitada;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;

/**
 * O backend não confia no navegador: valida de novo cada linha (anotações do ClienteDTO + regras
 * abaixo) e padroniza o básico. Só as linhas aprovadas seguem para o Python.
 */
@Service
public class ClienteValidacaoService {
  private final Validator validator;

  public ClienteValidacaoService(Validator validator) {
    this.validator = validator;
  }

  public record Resultado(List<ClienteDTO> validas, List<LinhaRejeitada> rejeitadas) {}

  public Resultado validar(List<ClienteDTO> linhas) {
    // Código repetido no mesmo lote: vale a primeira ocorrência, as seguintes são recusadas.
    Map<String, Integer> primeiraLinha = new HashMap<>();
    List<ClienteDTO> validas = new ArrayList<>();
    List<LinhaRejeitada> rejeitadas = new ArrayList<>();
    for (int i = 0; i < linhas.size(); i++) {
      ClienteDTO linha = linhas.get(i);
      if (linha == null) {
        rejeitadas.add(new LinhaRejeitada(i + 2, "", List.of("linha vazia")));
        continue;
      }
      List<String> motivos = new ArrayList<>();
      validator.validate(linha).stream()
          .map(ConstraintViolation::getMessage)
          .sorted()
          .forEach(motivos::add);
      int numero = linha.linha() == null ? i + 2 : linha.linha();
      if (linha.codigoCliente() != null && !linha.codigoCliente().isBlank()) {
        Integer anterior = primeiraLinha.putIfAbsent(chave(linha.codigoCliente()), numero);
        if (anterior != null) motivos.add("código do cliente já usado na linha " + anterior);
      }
      if (linha.dataContratacao() != null && linha.dataContratacao().matches("\\d{4}-\\d{2}-\\d{2}")) {
        try {
          if (LocalDate.parse(linha.dataContratacao()).isAfter(LocalDate.now()))
            motivos.add("data de contratação no futuro");
        } catch (DateTimeException error) {
          motivos.add("data de contratação inexistente");
        }
      }
      if (motivos.isEmpty()) validas.add(padronizar(linha, numero));
      else rejeitadas.add(new LinhaRejeitada(numero, texto(linha.codigoCliente()), motivos));
    }
    return new Resultado(validas, rejeitadas);
  }

  /** Padronização mínima no Java; a limpeza completa (segmento, nomes, serviços) é do Python. */
  private ClienteDTO padronizar(ClienteDTO l, int numero) {
    return new ClienteDTO(
        numero,
        texto(l.codigoCliente()).toUpperCase(Locale.ROOT),
        texto(l.nomeCliente()),
        texto(l.consultor()),
        texto(l.segmento()),
        texto(l.nivelCliente()).toUpperCase(Locale.ROOT),
        l.faturamentoAnual(),
        texto(l.servicosContratados()),
        l.dataContratacao(),
        texto(l.cidade()).isEmpty() ? null : texto(l.cidade()),
        texto(l.uf()).isEmpty() ? null : texto(l.uf()).toUpperCase(Locale.ROOT));
  }

  private static String chave(String codigo) {
    return codigo.trim().toUpperCase(Locale.ROOT);
  }

  private static String texto(String valor) {
    return valor == null ? "" : valor.trim().replaceAll("\\s+", " ");
  }
}
