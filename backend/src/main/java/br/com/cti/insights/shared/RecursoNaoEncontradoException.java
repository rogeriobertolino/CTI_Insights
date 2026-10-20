package br.com.cti.insights.shared;

public class RecursoNaoEncontradoException extends RuntimeException {
  public RecursoNaoEncontradoException(String recurso, Long id) {
    super(recurso + " não encontrado: " + id);
  }
}
