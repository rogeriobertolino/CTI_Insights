package com.cti.provedor.model;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class Usuario {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String nome;

  @Column(nullable = false, length = 254)
  private String email;

  @Column(nullable = false, length = 100)
  private String senhaHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "varchar(20) default 'USER'")
  private Perfil perfil = Perfil.USER;

  public Perfil getPerfil() {
    return perfil;
  }

  // DEFAULT APROVADO preserva os acessos que já existiam antes desta funcionalidade.
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "varchar(20) default 'APROVADO'")
  private StatusCadastro statusCadastro = StatusCadastro.APROVADO;

  @Column(nullable = false, columnDefinition = "timestamp with time zone default CURRENT_TIMESTAMP")
  private java.time.OffsetDateTime cadastradoEm = java.time.OffsetDateTime.now();

  private java.time.OffsetDateTime revisadoEm;

  @Column(length = 254)
  private String revisadoPor;

  public StatusCadastro getStatusCadastro() {
    return statusCadastro;
  }

  public java.time.OffsetDateTime getCadastradoEm() {
    return cadastradoEm;
  }

  public java.time.OffsetDateTime getRevisadoEm() {
    return revisadoEm;
  }

  public String getRevisadoPor() {
    return revisadoPor;
  }

  public void solicitarAprovacao() {
    statusCadastro = StatusCadastro.PENDENTE;
  }

  public void revisarCadastro(StatusCadastro decisao, String administrador) {
    statusCadastro = decisao;
    revisadoEm = java.time.OffsetDateTime.now();
    revisadoPor = administrador;
  }

  protected Usuario() {}

  public Usuario(String nome, String email, String senhaHash) {
    this.nome = nome;
    this.email = email;
    this.senhaHash = senhaHash;
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

  @com.fasterxml.jackson.annotation.JsonIgnore
  public String getSenhaHash() {
    return senhaHash;
  }
}
