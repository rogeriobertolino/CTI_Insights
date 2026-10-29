package com.cti.provedor.repository;

import com.cti.provedor.model.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {
  @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"cliente", "servico"})
  org.springframework.data.domain.Page<Contrato> findAll(
      org.springframework.data.domain.Pageable pageable);

  boolean existsByClienteIdAndServicoIdAndDataInicioAndValorAndStatus(
      Long clienteId,
      Long servicoId,
      java.time.LocalDate inicio,
      java.math.BigDecimal valor,
      com.cti.provedor.model.StatusContrato status);

  boolean existsByClienteId(Long id);

  boolean existsByServicoId(Long id);
}
