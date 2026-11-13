package com.cti.provedor.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** Cada planilha enviada. O Spring cria o lote; o Python (banco.py) atualiza o status. */
@Entity
@Table(name = "upload_lote")
public class UploadLote {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "arquivo_nome", nullable = false, length = 255)
  private String arquivoNome;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "varchar(20)")
  private StatusLote status = StatusLote.PENDENTE;

  @Column(name = "total_linhas", nullable = false)
  private int totalLinhas;

  @Column(name = "linhas_validas", nullable = false)
  private int linhasValidas;

  @Column(name = "linhas_invalidas", nullable = false)
  private int linhasInvalidas;

  @Column(name = "linhas_com_aviso", nullable = false)
  private int linhasComAviso;

  @Column(name = "linhas_rejeitadas_etl", nullable = false)
  private int linhasRejeitadasEtl;

  @Column(length = 500)
  private String mensagem;

  @Column(name = "enviado_por", length = 254)
  private String enviadoPor;

  @Column(name = "criado_em", nullable = false, columnDefinition = "timestamp with time zone")
  private OffsetDateTime criadoEm = OffsetDateTime.now();

  @Column(name = "processado_em", columnDefinition = "timestamp with time zone")
  private OffsetDateTime processadoEm;

  protected UploadLote() {}

  public UploadLote(
      String arquivoNome, int totalLinhas, int linhasValidas, int linhasInvalidas, int linhasComAviso,
      String enviadoPor) {
    this.arquivoNome = arquivoNome;
    this.totalLinhas = totalLinhas;
    this.linhasValidas = linhasValidas;
    this.linhasInvalidas = linhasInvalidas;
    this.linhasComAviso = linhasComAviso;
    this.enviadoPor = enviadoPor;
  }

  public Long getId() { return id; }
  public String getArquivoNome() { return arquivoNome; }
  public StatusLote getStatus() { return status; }
  public int getTotalLinhas() { return totalLinhas; }
  public int getLinhasValidas() { return linhasValidas; }
  public int getLinhasInvalidas() { return linhasInvalidas; }
  public int getLinhasComAviso() { return linhasComAviso; }
  public int getLinhasRejeitadasEtl() { return linhasRejeitadasEtl; }
  public String getMensagem() { return mensagem; }
  public String getEnviadoPor() { return enviadoPor; }
  public OffsetDateTime getCriadoEm() { return criadoEm; }
  public OffsetDateTime getProcessadoEm() { return processadoEm; }
}
