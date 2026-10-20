package br.com.cti.insights.clients;

public class ClienteDuplicadoException extends RuntimeException {
  public ClienteDuplicadoException(String codigoCti) {
    super("Já existe um cliente com o código CTI " + codigoCti + ".");
  }
}
