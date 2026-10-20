package br.com.cti.insights.services;

import br.com.cti.insights.domain.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicoRepository extends JpaRepository<Servico, Long> {
  java.util.Optional<Servico> findFirstByNomeIgnoreCaseOrderByIdAsc(String nome);
}
