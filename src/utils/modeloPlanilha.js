import { utils, write } from 'xlsx'

// Arquivos reais de exemplo para testar o mesmo caminho de leitura do upload.
export function criarArquivoExemplo(comProblemas = false) {
  const linhas = [
    ['codigo', 'nome', 'email', 'status', 'nivel', 'faturamento', 'segmento'],
    ['CLI001', 'Ana Silva', 'ana@example.com', 'Ativo', 'A', 1500, 'Serviços'],
    ['CLI002', 'Bruno Costa', 'bruno@example.com', 'Pendente', 'B', 2500, 'Comércio'],
  ]
  if (comProblemas) linhas.push(
    ['CLI003', '', 'carla.example.com', 'Ativo', 'A', 1500, 'Serviços'],
    ['CLI004', '  Diego  Lima ', 'DIEGO@EXAMPLE.COM', 'ativo', 'b'],
    ['CLI005', 'ELISA SOUZA', 'elisa@example.com', 'Desconhecido', 'D'],
    ['CLI006', 'Fábio Melo', 'fabio@example.com', 'Inativo', 'C'],
    ['cli006', 'Fernanda Melo', 'fernanda@example.com', 'Ativo', 'A', 1500, 'Serviços'],
  )
  const workbook = utils.book_new()
  const sheet = utils.aoa_to_sheet(linhas)
  sheet['!cols'] = [{ wch: 16 }, { wch: 25 }, { wch: 30 }, { wch: 18 }, { wch: 10 }]
  utils.book_append_sheet(workbook, sheet, 'Clientes')
  const bytes = write(workbook, { type: 'array', bookType: 'xlsx' })
  return new File([bytes], comProblemas ? 'exemplo-com-problemas.xlsx' : 'modelo-clientes.xlsx', { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
}
export function baixarModelo() {
  const arquivo = criarArquivoExemplo()
  baixarArquivo(arquivo)
}

export function baixarModeloAula() {
  const workbook = utils.book_new()
  utils.book_append_sheet(workbook, utils.aoa_to_sheet([
    ['codigo_cliente', 'cliente', 'consultor', 'segmento', 'nivel', 'faturamento_mensal', 'servico', 'valor_servico', 'status_contrato', 'data_inicio'],
    ['CLI001', 'Empresa Alfa', 'Maria Souza', 'Indústria', 'A', 1500, 'Internet empresarial', 300, 'ATIVO', '2026-01-15'],
    ['CLI002', 'Empresa Beta', 'João Lima', 'Comércio', 'B', 2500, 'Telefonia', 150, 'PENDENTE', '2026-02-10'],
    ['CLI003', 'Empresa Gama', 'Pedro Santos', 'Serviços', 'C', 0, 'Internet empresarial', 0, 'CANCELADO', '2026-03-20'],
  ]), 'dados_importacao')
  const bytes = write(workbook, { type: 'array', bookType: 'xlsx' })
  baixarArquivo(new File([bytes], 'CTI_Insights_modelo_oficial.xlsx', { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }))
}

function baixarArquivo(arquivo) {
  const url = URL.createObjectURL(arquivo)
  const link = document.createElement('a')
  link.href = url
  link.download = arquivo.name
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
