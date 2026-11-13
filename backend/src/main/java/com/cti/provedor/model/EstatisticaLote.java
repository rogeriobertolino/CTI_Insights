package com.cti.provedor.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** JSON das análises gravado pelo Python; o Spring só lê para o dashboard. */
@Entity
@Table(name = "estatistica_lote")
public class EstatisticaLote {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "lote_id", nullable = false, unique = true)
  private Long loteId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String resultado;

  @Column(name = "criado_em", nullable = false, columnDefinition = "timestamp with time zone")
  private OffsetDateTime criadoEm = OffsetDateTime.now();

  protected EstatisticaLote() {}

  public EstatisticaLote(Long loteId, String resultado) {
    this.loteId = loteId;
    this.resultado = resultado;
  }

  public Long getLoteId() { return loteId; }
  public String getResultado() { return resultado; }
  public OffsetDateTime getCriadoEm() { return criadoEm; }
}
