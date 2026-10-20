package br.com.cti.insights.imports;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, UUID> {
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select b from ImportBatch b where b.id = :id")
  Optional<ImportBatch> findForUpdate(
      @org.springframework.data.repository.query.Param("id") UUID id);

  Optional<ImportBatch> findFirstByStatusOrderByCreatedAtDesc(String status);
}
