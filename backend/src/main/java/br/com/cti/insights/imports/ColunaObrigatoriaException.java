package br.com.cti.insights.imports;

public class ColunaObrigatoriaException extends RuntimeException {
  public ColunaObrigatoriaException(String column) {
    super("A planilha deve conter a coluna obrigatória: " + column + ".");
  }
}
