package br.com.cti.insights.imports;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class ImportValidationService {
  public void validate(List<Map<String, Object>> records) {
    if (records.isEmpty()) throw new ExcelInvalidoException("A planilha não contém registros.");
    Set<String> codes = new HashSet<>();
    for (int i = 0; i < records.size(); i++) {
      Map<String, Object> row = records.get(i);
      String code = text(row, "codigo_cliente", "cliente_codigo", "cod_cliente", "codigo", "cnpj");
      String name = text(row, "nome_cliente", "razao_social", "nome", "cliente", "empresa");
      String segment = text(row, "segmento", "setor");
      String line = "Linha " + (i + 2) + ": ";
      if (code.isBlank() || name.isBlank() || segment.isBlank())
        throw new ExcelInvalidoException(line + "código, nome e segmento são obrigatórios.");
      String contact = text(row, "email", "email_cliente");
      String consultant = text(row, "consultor", "responsavel", "vendedor");
      if (code.length() > 80
          || name.length() > 180
          || segment.length() > 100
          || consultant.length() > 150)
        throw new ExcelInvalidoException(
            line + "código, nome, segmento ou consultor excede o tamanho permitido.");
      if (!contact.isBlank()
          && (contact.length() > 254 || !contact.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")))
        throw new ExcelInvalidoException(line + "e-mail inválido.");
      if (!codes.add(code.toLowerCase(Locale.ROOT)))
        throw new ExcelInvalidoException(line + "código duplicado.");
      boolean contractual =
          row.keySet().stream()
              .map(this::normalize)
              .anyMatch(Set.of("valor_servico", "status_contrato", "data_inicio")::contains);
      if (contractual) {
        if (consultant.isBlank()
            || text(row, "servico").isBlank()
            || text(row, "servico").length() > 150)
          throw new ExcelInvalidoException(
              line + "consultor e serviço são obrigatórios (serviço: até 150 caracteres).");
        if (!Set.of("ATIVO", "PENDENTE", "CANCELADO")
            .contains(text(row, "status_contrato").toUpperCase(Locale.ROOT)))
          throw new ExcelInvalidoException(line + "status do contrato inválido.");
        try {
          java.time.LocalDate.parse(text(row, "data_inicio"));
          BigDecimal value = new BigDecimal(text(row, "valor_servico"));
          if (value.signum() < 0 || value.scale() > 2 || value.precision() - value.scale() > 13)
            throw new IllegalArgumentException();
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException error) {
          throw new ExcelInvalidoException(line + "data ou valor do serviço inválido.");
        }
        if (text(row, "nivel", "nivel_cliente").isBlank()
            || text(row, "faturamento_mensal").isBlank())
          throw new ExcelInvalidoException(line + "nível e faturamento mensal são obrigatórios.");
      }
      String level = text(row, "nivel", "nivel_cliente", "classificacao");
      if (!level.isBlank() && !Set.of("A", "B", "C").contains(level.toUpperCase(Locale.ROOT)))
        throw new ExcelInvalidoException(line + "nível deve ser A, B ou C.");
      for (String key : row.keySet())
        if (Set.of("faturamento", "faturamento_mensal", "faturamento_anual", "receita")
            .contains(normalize(key))) {
          String value = String.valueOf(row.get(key)).trim().replace("R$", "").replace(" ", "");
          if (value.contains(",") && value.contains(".")) {
            if (value.lastIndexOf(',') > value.lastIndexOf('.'))
              value = value.replace(".", "").replace(",", ".");
            else value = value.replace(",", "");
          } else value = value.replace(",", ".");
          try {
            if (!value.matches("[+-]?\\d+(?:\\.\\d+)?") || new BigDecimal(value).signum() < 0)
              throw new NumberFormatException();
          } catch (NumberFormatException error) {
            throw new ExcelInvalidoException(
                line + "faturamento deve ser numérico e não negativo.");
          }
        }
    }
  }

  private String text(Map<String, Object> row, String... aliases) {
    for (String alias : aliases)
      for (String key : row.keySet())
        if (normalize(key).equals(alias))
          return row.get(key) == null ? "" : row.get(key).toString().trim();
    return "";
  }

  private String normalize(String key) {
    return Normalizer.normalize(key, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "_")
        .replaceAll("^_|_$", "");
  }
}
