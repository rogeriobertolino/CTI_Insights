export const cadastrosComerciais = {
  consultores: {
    titulo: 'Consultores', campos: [
      { nome: 'nome', rotulo: 'Nome', obrigatorio: true, max: 150 },
      { nome: 'email', rotulo: 'E-mail', tipo: 'email', obrigatorio: true, max: 150 },
      { nome: 'telefone', rotulo: 'Telefone', max: 30 }, { nome: 'matricula', rotulo: 'Matrícula', max: 50 },
    ],
  },
  clientes: {
    titulo: 'Clientes', campos: [
      { nome: 'codigoCti', rotulo: 'Código CTI', obrigatorio: true, max: 80 },
      { nome: 'consultorId', rotulo: 'ID do consultor', tipo: 'id', obrigatorio: true },
      { nome: 'nomeEmpresa', rotulo: 'Nome da empresa', obrigatorio: true, max: 180 },
      { nome: 'segmento', rotulo: 'Segmento', obrigatorio: true, max: 100 },
      { nome: 'email', rotulo: 'E-mail de contato', tipo: 'email', max: 254 },
      { nome: 'faturamentoAnual', rotulo: 'Faturamento anual (R$)', tipo: 'moeda', obrigatorio: true },
      { nome: 'nivel', rotulo: 'Nível', opcoes: ['A', 'B', 'C'], obrigatorio: true },
      { nome: 'status', rotulo: 'Situação', opcoes: ['ATIVO', 'INATIVO'], obrigatorio: true },
    ],
  },
  servicos: {
    titulo: 'Serviços', campos: [
      { nome: 'nome', rotulo: 'Nome', obrigatorio: true, max: 150 },
      { nome: 'categoria', rotulo: 'Categoria', max: 100 },
      { nome: 'descricao', rotulo: 'Descrição', tipo: 'texto', max: 4000 },
      { nome: 'preco', rotulo: 'Preço de referência (R$)', tipo: 'moeda' },
    ],
  },
  contratos: {
    titulo: 'Contratos', campos: [
      { nome: 'clienteId', rotulo: 'ID do cliente', tipo: 'id', obrigatorio: true },
      { nome: 'servicoId', rotulo: 'ID do serviço', tipo: 'id', obrigatorio: true },
      { nome: 'dataInicio', rotulo: 'Data inicial', tipo: 'date', obrigatorio: true },
      { nome: 'dataFim', rotulo: 'Data final', tipo: 'date' },
      { nome: 'valor', rotulo: 'Valor (R$)', tipo: 'moeda', obrigatorio: true, min: 0.01 },
      { nome: 'status', rotulo: 'Situação', opcoes: ['ATIVO', 'PENDENTE', 'ENCERRADO', 'CANCELADO'], obrigatorio: true },
    ],
  },
  insights: {
    titulo: 'Insights', campos: [
      { nome: 'tipo', rotulo: 'Tipo', obrigatorio: true, max: 100 },
      { nome: 'descricao', rotulo: 'Descrição', tipo: 'texto', obrigatorio: true, max: 4000 },
      { nome: 'clienteId', rotulo: 'ID do cliente (opcional)', tipo: 'id' },
      { nome: 'contratoId', rotulo: 'ID do contrato (opcional)', tipo: 'id' },
    ],
  },
  telemetrias: {
    titulo: 'Eventos de importação', somenteLeitura: true, campos: [
      { nome: 'arquivoNome', rotulo: 'Arquivo' }, { nome: 'dataHora', rotulo: 'Quando' },
      { nome: 'tipoEvento', rotulo: 'Evento' }, { nome: 'status', rotulo: 'Resultado' },
      { nome: 'usuario', rotulo: 'Responsável' }, { nome: 'mensagemErro', rotulo: 'Observação' },
    ],
  },
}
export function formularioComercial(nome, registro = {}) {
  return Object.fromEntries(cadastrosComerciais[nome].campos.map(campo => [campo.nome, registro[campo.nome] ?? campo.opcoes?.[0] ?? '']))
}
export function payloadComercial(nome, formulario) {
  return Object.fromEntries(cadastrosComerciais[nome].campos.map(campo => {
    let valor = formulario[campo.nome]
    if (valor === '' || valor == null) valor = null
    else if (['id', 'moeda'].includes(campo.tipo)) valor = Number(valor)
    else if (typeof valor === 'string') valor = valor.trim()
    return [campo.nome, valor]
  }))
}
