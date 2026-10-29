package com.cti.provedor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "consultor")
public class Consultor {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_consultor")
  private Long id;

  @Column(nullable = false, length = 150)
  private String nome;

  @Column(nullable = false, unique = true, length = 150)
  private String email;

  @Column(length = 30)
  private String telefone;

  @Column(length = 50)
  private String matricula;

  public String getMatricula() {
    return matricula;
  }

  public void update(String nome, String email, String telefone, String matricula) {
    this.nome = nome;
    this.email = email;
    this.telefone = telefone;
    this.matricula = matricula;
  }

  protected Consultor() {}

  public Consultor(String nome, String email, String telefone) {
    this.nome = nome;
    this.email = email;
    this.telefone = telefone;
  }

  public Long getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public String getEmail() {
    return email;
  }

  public String getTelefone() {
    return telefone;
  }
}
