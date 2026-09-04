package br.com.cti.insights.imports;

import org.springframework.data.jpa.repository.JpaRepository;
public interface ImportedRecordRepository extends JpaRepository<ImportedRecord,Long> {}
