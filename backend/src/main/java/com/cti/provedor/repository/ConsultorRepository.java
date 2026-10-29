package com.cti.provedor.repository;

import com.cti.provedor.model.Consultor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultorRepository extends JpaRepository<Consultor, Long> {
  Optional<Consultor> findByNome(String nome);

  Optional<Consultor> findFirstByNomeIgnoreCaseOrderByIdAsc(String nome);

  Optional<Consultor> findByEmailIgnoreCase(String email);
}
