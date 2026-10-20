package br.com.cti.insights.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
  Optional<Usuario> findByEmail(String email);

  boolean existsByEmail(String email);

  org.springframework.data.domain.Page<Usuario> findByPerfil(
      Perfil perfil, org.springframework.data.domain.Pageable pageable);

  org.springframework.data.domain.Page<Usuario> findByPerfilAndStatusCadastro(
      Perfil perfil, StatusCadastro status, org.springframework.data.domain.Pageable pageable);

  long countByPerfilAndStatusCadastro(Perfil perfil, StatusCadastro status);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.id=:id")
  Optional<Usuario> findForReview(@org.springframework.data.repository.query.Param("id") Long id);
}
