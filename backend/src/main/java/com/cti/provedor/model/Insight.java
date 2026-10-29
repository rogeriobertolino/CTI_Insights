package com.cti.provedor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "insight")
public class Insight {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_insight")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_cliente")
  private Cliente cliente;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_contrato")
  private Contrato contrato;

  @Column(nullable = false, length = 100)
  private String tipo;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String descricao;

  @Column(name = "data_geracao", nullable = false)
  private LocalDateTime dataGeracao = LocalDateTime.now();

  public Insight() {}

  public Long getId() {
    return id;
  }

  public Cliente getCliente() {
    return cliente;
  }

  public Contrato getContrato() {
    return contrato;
  }

  public String getTipo() {
    return tipo;
  }

  public String getDescricao() {
    return descricao;
  }

  public LocalDateTime getDataGeracao() {
    return dataGeracao;
  }

  public void update(Cliente cliente, Contrato contrato, String tipo, String descricao) {
    this.cliente = cliente;
    this.contrato = contrato;
    this.tipo = tipo;
    this.descricao = descricao;
  }
}
