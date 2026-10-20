package br.com.cti.insights.imports;

import br.com.cti.insights.clients.ClienteRepository;
import br.com.cti.insights.clients.ConsultorRepository;
import br.com.cti.insights.domain.*;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportDomainService {
  private final ClienteRepository clientes;
  private final ConsultorRepository consultores;

  private final br.com.cti.insights.services.ServicoRepository servicos;
  private final br.com.cti.insights.contracts.ContratoRepository contratos;

  public ImportDomainService(
      ClienteRepository clientes,
      ConsultorRepository consultores,
      br.com.cti.insights.services.ServicoRepository servicos,
      br.com.cti.insights.contracts.ContratoRepository contratos) {
    this.servicos = servicos;
    this.contratos = contratos;
    this.clientes = clientes;
    this.consultores = consultores;
  }

  @Transactional
  public int persistClients(List<Map<String, Object>> records) {
    int created = 0;
    for (Map<String, Object> record : records) {
      String name = text(record, "razao_social", "nome_cliente", "nome", "cliente", "empresa");
      String code =
          text(record, "cliente_codigo", "codigo_cliente", "cod_cliente", "codigo", "cnpj");
      if (code.isBlank()) code = name;
      if (code.isBlank()) continue;
      var existing = clientes.findByCodigoCti(code);
      if (existing.isPresent()) {
        persistContract(record, existing.get());
        continue;
      }

      String consultantName = text(record, "consultor", "responsavel", "vendedor");
      if (consultantName.isBlank()) consultantName = "Importação CTI";
      String resolvedConsultantName = consultantName;
      // O e-mail gerado identifica cadastros antigos mesmo com diferenças de caixa,
      // acentos ou espaços no nome. Reutilize-os antes de tentar inserir.
      Consultor consultant =
          consultores
              .findByEmailIgnoreCase(emailFor(resolvedConsultantName))
              .or(() -> consultores.findFirstByNomeIgnoreCaseOrderByIdAsc(resolvedConsultantName))
              .orElseGet(
                  () ->
                      consultores.save(
                          new Consultor(
                              resolvedConsultantName, emailFor(resolvedConsultantName), null)));

      NivelCliente level = parseLevel(text(record, "nivel", "nivel_cliente", "classificacao"));
      String monthly = exactText(record, "faturamento_mensal");
      BigDecimal revenue =
          monthly.isBlank()
              ? parseMoney(text(record, "faturamento_anual", "faturamento", "receita"))
              : parseMoney(monthly).multiply(BigDecimal.valueOf(12));
      Cliente client =
          new Cliente(
              code,
              consultant,
              name.isBlank() ? code : name,
              defaultText(text(record, "segmento", "setor"), "Não informado"),
              revenue,
              level,
              StatusCliente.ATIVO);
      String contact = exactText(record, "email");
      if (contact.isBlank()) contact = exactText(record, "email_cliente");
      if (!contact.isBlank()) client.setEmail(contact.toLowerCase(java.util.Locale.ROOT));
      clientes.save(client);
      persistContract(record, client);
      created++;
    }
    return created;
  }

  private void persistContract(Map<String, Object> row, Cliente client) {
    String serviceName = exactText(row, "servico");
    String start = exactText(row, "data_inicio");
    if (serviceName.isBlank() || start.isBlank()) return;
    var price = parseMoney(exactText(row, "valor_servico"));
    var date = java.time.LocalDate.parse(start);
    var status =
        StatusContrato.valueOf(
            exactText(row, "status_contrato").toUpperCase(java.util.Locale.ROOT));
    var service =
        servicos
            .findFirstByNomeIgnoreCaseOrderByIdAsc(serviceName)
            .orElseGet(
                () -> {
                  var item = new Servico(serviceName, null, null);
                  item.update(serviceName, null, null, price);
                  return servicos.save(item);
                });
    if (!contratos.existsByClienteIdAndServicoIdAndDataInicioAndValorAndStatus(
        client.getId(), service.getId(), date, price, status)) {
      var contract = new Contrato();
      contract.update(client, service, date, null, price, status);
      contratos.save(contract);
    }
  }

  private String exactText(Map<String, Object> record, String alias) {
    for (String key : record.keySet())
      if (normalize(key).equals(normalize(alias)))
        return record.get(key) == null ? "" : record.get(key).toString().trim();
    return "";
  }

  private String text(Map<String, Object> record, String... aliases) {
    for (String alias : aliases) {
      String exact = exactText(record, alias);
      if (!exact.isBlank()) return exact;
    }
    for (String key : record.keySet()) {
      String normalized = normalize(key);
      for (String alias : aliases) {
        if (normalized.equals(normalize(alias)) || normalized.contains(normalize(alias))) {
          Object value = record.get(key);
          return value == null ? "" : value.toString().trim();
        }
      }
    }
    return "";
  }

  private String normalize(String value) {
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase()
        .replaceAll("[^a-z0-9]", "");
  }

  private String defaultText(String value, String fallback) {
    return value.isBlank() ? fallback : value;
  }

  private String emailFor(String name) {
    String slug = normalize(name);
    if (slug.length() > 140) slug = slug.substring(0, 140);
    return (slug.isBlank() ? "importacao" : slug) + "@cti.local";
  }

  private NivelCliente parseLevel(String value) {
    try {
      return NivelCliente.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException exception) {
      return NivelCliente.C;
    }
  }

  private BigDecimal parseMoney(String value) {
    if (value.isBlank()) return BigDecimal.ZERO;
    try {
      String normalized = value.replace("R$", "").replace(" ", "");
      if (normalized.contains(",") && normalized.contains(".")) {
        normalized =
            normalized.lastIndexOf(',') > normalized.lastIndexOf('.')
                ? normalized.replace(".", "").replace(",", ".")
                : normalized.replace(",", "");
      } else normalized = normalized.replace(",", ".");
      return new BigDecimal(normalized);
    } catch (NumberFormatException exception) {
      return BigDecimal.ZERO;
    }
  }
}
