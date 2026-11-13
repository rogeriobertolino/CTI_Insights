package com.cti.provedor.repository;

import com.cti.provedor.model.EstatisticaLote;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstatisticaLoteRepository extends JpaRepository<EstatisticaLote, Long> {
  Optional<EstatisticaLote> findByLoteId(Long loteId);
}
