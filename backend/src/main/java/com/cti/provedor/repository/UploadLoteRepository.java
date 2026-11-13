package com.cti.provedor.repository;

import com.cti.provedor.model.StatusLote;
import com.cti.provedor.model.UploadLote;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface UploadLoteRepository extends JpaRepository<UploadLote, Long> {
  List<UploadLote> findTop50ByOrderByIdDesc();

  Optional<UploadLote> findFirstByStatusOrderByIdDesc(StatusLote status);

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Modifying(clearAutomatically = true)
  @Query(
      "update UploadLote l set l.status = :erro, l.mensagem = :mensagem, l.processadoEm = :agora"
          + " where l.id = :id and l.status in :abertos")
  int atualizarParaErro(
      @Param("id") Long id,
      @Param("mensagem") String mensagem,
      @Param("agora") OffsetDateTime agora,
      @Param("erro") StatusLote erro,
      @Param("abertos") Collection<StatusLote> abertos);

  /**
   * Roda depois do commit do upload, por isso usa uma transação própria. Não sobrescreve um lote
   * que o Python já concluiu ou já marcou com erro.
   */
  default int marcarErro(Long id, String mensagem) {
    return atualizarParaErro(
        id, mensagem, OffsetDateTime.now(), StatusLote.ERRO,
        List.of(StatusLote.PENDENTE, StatusLote.PROCESSANDO));
  }
}
