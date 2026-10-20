package br.com.cti.insights.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "telemetria",
    indexes = @Index(name = "idx_telemetria_consultor", columnList = "id_consultor"))
public class Telemetria {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_telemetria")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_consultor")
  private Consultor consultor;

  @Column(name = "arquivo_nome", nullable = false, length = 255)
  private String arquivoNome;

  @Column(name = "data_hora", nullable = false)
  private LocalDateTime dataHora = LocalDateTime.now();

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_evento", nullable = false, length = 20)
  private TipoEventoTelemetria tipoEvento;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private StatusTelemetria status;

  @Column(name = "mensagem_erro", columnDefinition = "TEXT")
  private String mensagemErro;

  @Column(length = 254)
  private String usuario;

  public Telemetria(
      String arquivoNome,
      TipoEventoTelemetria tipoEvento,
      StatusTelemetria status,
      String mensagemErro,
      String usuario) {
    this.arquivoNome = arquivoNome;
    this.tipoEvento = tipoEvento;
    this.status = status;
    this.mensagemErro = mensagemErro;
    this.usuario = usuario;
  }

  public Long getId() {
    return id;
  }

  public String getArquivoNome() {
    return arquivoNome;
  }

  public LocalDateTime getDataHora() {
    return dataHora;
  }

  public TipoEventoTelemetria getTipoEvento() {
    return tipoEvento;
  }

  public StatusTelemetria getStatus() {
    return status;
  }

  public String getMensagemErro() {
    return mensagemErro;
  }

  public String getUsuario() {
    return usuario;
  }

  protected Telemetria() {}
}
