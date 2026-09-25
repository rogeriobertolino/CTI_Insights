package br.com.cti.insights.clients;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.cti.insights.domain.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByCodigoCti(String codigoCti);
    boolean existsByCodigoCtiAndIdNot(String codigoCti, Long id);
    Optional<Cliente> findByCodigoCti(String codigoCti);
}
