package com.cti.provedor.service;

public class ClienteDuplicadoException extends RuntimeException {
  public ClienteDuplicadoException(String codigoCti) {
    super("Já existe um cliente com o código CTI " + codigoCti + ".");
  }
}
