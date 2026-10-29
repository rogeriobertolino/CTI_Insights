package com.cti.provedor.repository;

import com.cti.provedor.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicoRepository extends JpaRepository<Servico, Long> {
  java.util.Optional<Servico> findFirstByNomeIgnoreCaseOrderByIdAsc(String nome);
}
