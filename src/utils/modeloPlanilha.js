import { utils, write } from 'xlsx'

// Arquivos reais de exemplo para testar o mesmo caminho de leitura do upload.
export function criarArquivoExemplo(comProblemas = false) {
  const linhas = [
    ['codigo', 'nome', 'email', 'status', 'nivel'],
    ['CLI001', 'Ana Silva', 'ana@example.com', 'Ativo', 'A'],
    ['CLI002', 'Bruno Costa', 'bruno@example.com', 'Pendente', 'B'],
  ]
  if (comProblemas) linhas.push(
    ['CLI003', '', 'carla.example.com', 'Ativo', 'A'],
    ['CLI004', '  Diego  Lima ', 'DIEGO@EXAMPLE.COM', 'ativo', 'b'],
    ['CLI005', 'ELISA SOUZA', 'elisa@example.com', 'Desconhecido', 'D'],
    ['CLI006', 'Fábio Melo', 'fabio@example.com', 'Inativo', 'C'],
    ['cli006', 'Fernanda Melo', 'fernanda@example.com', 'Ativo', 'A'],
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
  const url = URL.createObjectURL(arquivo)
  const link = document.createElement('a')
  link.href = url
  link.download = arquivo.name
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
