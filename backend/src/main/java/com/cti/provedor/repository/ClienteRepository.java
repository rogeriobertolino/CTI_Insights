package com.cti.provedor.repository;

import com.cti.provedor.model.Cliente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository
    extends JpaRepository<Cliente, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Cliente> {
  @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "consultor")
  org.springframework.data.domain.Page<Cliente> findAll(
      org.springframework.data.jpa.domain.Specification<Cliente> spec,
      org.springframework.data.domain.Pageable pageable);

  @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "consultor")
  java.util.List<Cliente> findAll();

  boolean existsByConsultorId(Long consultorId);

  boolean existsByCodigoCti(String codigoCti);

  boolean existsByCodigoCtiAndIdNot(String codigoCti, Long id);

  Optional<Cliente> findByCodigoCti(String codigoCti);
}
