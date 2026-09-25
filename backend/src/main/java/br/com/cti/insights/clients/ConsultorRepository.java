package br.com.cti.insights.clients;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.cti.insights.domain.Consultor;
import java.util.Optional;

public interface ConsultorRepository extends JpaRepository<Consultor, Long> {
	Optional<Consultor> findByNome(String nome);
}
