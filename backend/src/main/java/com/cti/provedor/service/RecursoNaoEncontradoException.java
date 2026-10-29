package com.cti.provedor.service;

public class RecursoNaoEncontradoException extends RuntimeException {
  public RecursoNaoEncontradoException(String recurso, Long id) {
    super(recurso + " não encontrado: " + id);
  }
}
