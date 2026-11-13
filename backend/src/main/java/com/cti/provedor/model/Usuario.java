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
  @Column(length = 64)
  private String resetPasswordTokenHash;
  private java.time.OffsetDateTime resetPasswordExpiresAt;
  private java.time.OffsetDateTime resetPasswordRequestedAt;
  @Column(length = 30) private String matricula;
  @Column(length = 60) private String turma;
  public String getMatricula() { return matricula; }
  public String getTurma() { return turma; }
  public void setDadosAcademicos(String matricula, String turma) { this.matricula = matricula; this.turma = turma; }

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "varchar(20)")
  @org.hibernate.annotations.ColumnDefault("'USER'")
  private Perfil perfil = Perfil.USER;

  public Perfil getPerfil() {
    return perfil;
  }

  // DEFAULT APROVADO preserva os acessos que já existiam antes desta funcionalidade.
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "varchar(20)")
  @org.hibernate.annotations.ColumnDefault("'APROVADO'")
  private StatusCadastro statusCadastro = StatusCadastro.APROVADO;

  @Column(nullable = false, columnDefinition = "timestamp with time zone")
  @org.hibernate.annotations.ColumnDefault("CURRENT_TIMESTAMP")
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

  public void iniciarRedefinicaoSenha(
      String tokenHash, java.time.OffsetDateTime expiraEm, java.time.OffsetDateTime solicitadoEm) {
    resetPasswordTokenHash = tokenHash;
    resetPasswordExpiresAt = expiraEm;
    resetPasswordRequestedAt = solicitadoEm;
  }

  public java.time.OffsetDateTime getResetPasswordRequestedAt() {
    return resetPasswordRequestedAt;
  }

  public void redefinirSenha(String novaSenhaHash) {
    senhaHash = novaSenhaHash;
    limparRedefinicaoSenha();
  }

  public void limparRedefinicaoSenha() {
    resetPasswordTokenHash = null;
    resetPasswordExpiresAt = null;
    resetPasswordRequestedAt = null;
  }

  public String getResetPasswordTokenHash() {
    return resetPasswordTokenHash;
  }

  public java.time.OffsetDateTime getResetPasswordExpiresAt() {
    return resetPasswordExpiresAt;
  }

  @com.fasterxml.jackson.annotation.JsonIgnore
  public String getSenhaHash() {
    return senhaHash;
  }
}
