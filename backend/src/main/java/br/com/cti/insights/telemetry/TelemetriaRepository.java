package br.com.cti.insights.telemetry;

import br.com.cti.insights.domain.Telemetria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelemetriaRepository extends JpaRepository<Telemetria, Long> {}
