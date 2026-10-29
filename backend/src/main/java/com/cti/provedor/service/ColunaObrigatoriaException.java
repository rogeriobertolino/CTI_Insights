package com.cti.provedor.service;

public class ColunaObrigatoriaException extends RuntimeException {
  public ColunaObrigatoriaException(String column) {
    super("A planilha deve conter a coluna obrigatória: " + column + ".");
  }
}
