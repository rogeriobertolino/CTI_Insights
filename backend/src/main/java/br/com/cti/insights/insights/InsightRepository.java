package br.com.cti.insights.insights;

import br.com.cti.insights.domain.Insight;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsightRepository extends JpaRepository<Insight, Long> {
  java.util.List<Insight> findByTipoAndDescricaoStartingWith(String tipo, String descricao);

  boolean existsByClienteId(Long id);

  boolean existsByContratoId(Long id);
}
