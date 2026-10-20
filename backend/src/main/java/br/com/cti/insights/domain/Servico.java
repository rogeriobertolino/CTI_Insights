package br.com.cti.insights.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "servico")
public class Servico {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_servico")
  private Long id;

  @Column(nullable = false, length = 150)
  private String nome;

  @Column(columnDefinition = "TEXT")
  private String descricao;

  @Column(length = 100)
  private String categoria;

  @Column(precision = 15, scale = 2)
  private java.math.BigDecimal preco;

  public java.math.BigDecimal getPreco() {
    return preco;
  }

  public void update(String nome, String descricao, String categoria, java.math.BigDecimal preco) {
    this.nome = nome;
    this.descricao = descricao;
    this.categoria = categoria;
    this.preco = preco;
  }

  protected Servico() {}

  public Servico(String nome, String descricao, String categoria) {
    this.nome = nome;
    this.descricao = descricao;
    this.categoria = categoria;
  }

  public Long getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public String getDescricao() {
    return descricao;
  }

  public String getCategoria() {
    return categoria;
  }
}
