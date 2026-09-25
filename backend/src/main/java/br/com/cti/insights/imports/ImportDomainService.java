package br.com.cti.insights.imports;

import br.com.cti.insights.clients.ClienteRepository;
import br.com.cti.insights.clients.ConsultorRepository;
import br.com.cti.insights.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;

@Service
public class ImportDomainService {
    private final ClienteRepository clientes;
    private final ConsultorRepository consultores;

    public ImportDomainService(ClienteRepository clientes, ConsultorRepository consultores) {
        this.clientes = clientes;
        this.consultores = consultores;
    }

    @Transactional
    public int persistClients(List<Map<String, Object>> records) {
        int created = 0;
        for (Map<String, Object> record : records) {
            String name = text(record, "razao_social", "nome_cliente", "cliente", "empresa");
            String code = text(record, "cliente_codigo", "codigo_cliente", "cod_cliente", "cnpj");
            if (code.isBlank()) code = name;
            if (code.isBlank() || clientes.existsByCodigoCti(code)) continue;

            String consultantName = text(record, "consultor", "responsavel", "vendedor");
            if (consultantName.isBlank()) consultantName = "Importação CTI";
            String resolvedConsultantName = consultantName;
            Consultor consultant = consultores.findByNome(resolvedConsultantName).orElseGet(() ->
                consultores.save(new Consultor(resolvedConsultantName, emailFor(resolvedConsultantName), null)));

            NivelCliente level = parseLevel(text(record, "nivel", "classificacao"));
            BigDecimal revenue = parseMoney(text(record, "faturamento_anual", "faturamento", "receita"));
            Cliente client = new Cliente(code, consultant, name.isBlank() ? code : name,
                defaultText(text(record, "segmento", "setor"), "Não informado"), revenue, level, StatusCliente.ATIVO);
            clientes.save(client);
            created++;
        }
        return created;
    }

    private String text(Map<String, Object> record, String... aliases) {
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
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
            .toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private String defaultText(String value, String fallback) { return value.isBlank() ? fallback : value; }

    private String emailFor(String name) {
        String slug = normalize(name);
        return (slug.isBlank() ? "importacao" : slug) + "@cti.local";
    }

    private NivelCliente parseLevel(String value) {
        try { return NivelCliente.valueOf(value.trim().toUpperCase()); }
        catch (IllegalArgumentException exception) { return NivelCliente.C; }
    }

    private BigDecimal parseMoney(String value) {
        if (value.isBlank()) return BigDecimal.ZERO;
        try {
            String normalized = value.replace("R$", "").replace(" ", "");
            if (normalized.contains(",")) normalized = normalized.replace(".", "").replace(",", ".");
            return new BigDecimal(normalized);
        } catch (NumberFormatException exception) { return BigDecimal.ZERO; }
    }
}
