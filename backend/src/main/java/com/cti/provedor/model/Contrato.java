package com.cti.provedor.model;

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
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "contrato",
    indexes = {
      @Index(name = "idx_contrato_cliente", columnList = "id_cliente"),
      @Index(name = "idx_contrato_servico", columnList = "id_servico")
    })
public class Contrato {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_contrato")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_cliente", nullable = false)
  private Cliente cliente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_servico", nullable = false)
  private Servico servico;

  @Column(name = "data_inicio", nullable = false)
  private LocalDate dataInicio;

  @Column(name = "data_fim")
  private LocalDate dataFim;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal valor;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private StatusContrato status = StatusContrato.ATIVO;

  public Contrato() {}

  public Long getId() {
    return id;
  }

  public Cliente getCliente() {
    return cliente;
  }

  public Servico getServico() {
    return servico;
  }

  public LocalDate getDataInicio() {
    return dataInicio;
  }

  public LocalDate getDataFim() {
    return dataFim;
  }

  public BigDecimal getValor() {
    return valor;
  }

  public StatusContrato getStatus() {
    return status;
  }

  public void update(
      Cliente cliente,
      Servico servico,
      LocalDate inicio,
      LocalDate fim,
      BigDecimal valor,
      StatusContrato status) {
    this.cliente = cliente;
    this.servico = servico;
    this.dataInicio = inicio;
    this.dataFim = fim;
    this.valor = valor;
    this.status = status;
  }
}
