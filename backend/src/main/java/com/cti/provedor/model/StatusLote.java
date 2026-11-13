package com.cti.provedor.model;

/** Ciclo do lote: o Spring cria PENDENTE; o Python marca PROCESSANDO e PROCESSADO; falha vira ERRO. */
public enum StatusLote {
  PENDENTE,
  PROCESSANDO,
  PROCESSADO,
  ERRO
}
