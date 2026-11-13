"""Único arquivo que fala com o banco: reserva o lote, grava o resultado ou marca erro.

As tabelas cliente, consultor, servico e contrato são as mesmas que o Spring Boot
usa nas telas de gestão comercial; o Python só acrescenta a coluna lote_id.
"""
import json
import re
import unicodedata

from sqlalchemy import create_engine, text

from config import database_url

TABELAS = [
    """CREATE TABLE IF NOT EXISTS upload_lote (
        id BIGSERIAL PRIMARY KEY,
        arquivo_nome VARCHAR(255) NOT NULL,
        status VARCHAR(20) NOT NULL,
        total_linhas INTEGER NOT NULL,
        linhas_validas INTEGER NOT NULL,
        linhas_invalidas INTEGER NOT NULL,
        linhas_com_aviso INTEGER NOT NULL,
        linhas_rejeitadas_etl INTEGER NOT NULL DEFAULT 0,
        mensagem VARCHAR(500),
        enviado_por VARCHAR(254),
        criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        processado_em TIMESTAMP WITH TIME ZONE)""",
    """CREATE TABLE IF NOT EXISTS segmento (
        id BIGSERIAL PRIMARY KEY,
        nome VARCHAR(100) NOT NULL UNIQUE)""",
    """CREATE TABLE IF NOT EXISTS estatistica_lote (
        id BIGSERIAL PRIMARY KEY,
        lote_id BIGINT NOT NULL UNIQUE REFERENCES upload_lote(id),
        resultado TEXT NOT NULL,
        criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP)""",
    "ALTER TABLE cliente ADD COLUMN IF NOT EXISTS lote_id BIGINT",
    "ALTER TABLE contrato ADD COLUMN IF NOT EXISTS lote_id BIGINT",
]


def criar_engine(url=None):
    """Abre a conexão com o banco do .env (pool_pre_ping acorda o Neon que dormiu)."""
    return create_engine(url or database_url(), pool_pre_ping=True)


def garantir_tabelas(engine):
    """Cria as tabelas do ETL se ainda não existirem (o Spring cria cliente/contrato)."""
    with engine.begin() as conn:
        for ddl in TABELAS:
            conn.execute(text(ddl))


def criar_lote(engine, arquivo_nome, recebidas, invalidas=0, com_aviso=0, enviado_por=None):
    """Cria PENDENTE para que processar_lote possa reservar o novo lote."""
    with engine.begin() as conn:
        return conn.execute(text(
            "INSERT INTO upload_lote (arquivo_nome, status, total_linhas, linhas_validas, linhas_invalidas,"
            " linhas_com_aviso, linhas_rejeitadas_etl, enviado_por, criado_em)"
            " VALUES (:nome, 'PENDENTE', :total, 0, :inv, :aviso, 0, :por, now()) RETURNING id"),
            {"nome": arquivo_nome[:255], "total": recebidas, "inv": invalidas, "aviso": com_aviso, "por": enviado_por},
        ).scalar_one()


def reservar_lote(engine, lote_id):
    """Marca PENDENTE -> PROCESSANDO. Devolve False se outro processo já pegou o lote."""
    with engine.begin() as conn:
        return conn.execute(text(
            "UPDATE upload_lote SET status = 'PROCESSANDO', mensagem = NULL WHERE id = :id AND status = 'PENDENTE'"),
            {"id": lote_id}).rowcount == 1


def _email_consultor(nome):
    # Mesmo e-mail gerado pelo Java (ImportDomainService) para não duplicar consultores.
    slug = re.sub(r"[^a-z0-9]", "", unicodedata.normalize("NFD", nome).encode("ascii", "ignore").decode().lower())
    return (slug[:140] or "importacao") + "@cti.local"


def _id_consultor(conn, nome):
    email = _email_consultor(nome)
    achado = conn.execute(text(
        "SELECT id_consultor FROM consultor WHERE lower(email) = lower(:email) OR lower(nome) = lower(:nome)"
        " ORDER BY id_consultor LIMIT 1"), {"email": email, "nome": nome}).scalar()
    if achado:
        return achado
    return conn.execute(text("INSERT INTO consultor (nome, email) VALUES (:nome, :email) RETURNING id_consultor"),
                        {"nome": nome[:150], "email": email}).scalar_one()


def _id_servico(conn, nome):
    achado = conn.execute(text("SELECT id_servico FROM servico WHERE lower(nome) = lower(:nome) ORDER BY id_servico LIMIT 1"),
                          {"nome": nome}).scalar()
    if achado:
        return achado
    return conn.execute(text("INSERT INTO servico (nome) VALUES (:nome) RETURNING id_servico"), {"nome": nome[:150]}).scalar_one()


def gravar_resultado(engine, lote_id, res, resultado):
    """Grava tudo em UMA transação: se algo falhar no meio, o banco desfaz (ROLLBACK)."""
    df = res.tratado
    with engine.begin() as conn:
        for nome in sorted(df["segmento"].unique()):
            conn.execute(text("INSERT INTO segmento (nome) VALUES (:n) ON CONFLICT (nome) DO NOTHING"), {"n": nome})
        consultores = {nome: _id_consultor(conn, nome) for nome in df["consultor"].unique()}
        servicos = {nome: _id_servico(conn, nome) for lista in df["servicos"] for nome in lista}
        for linha in df.itertuples():
            # :lote e não texto colado: evita SQL injection e erro com aspas no nome.
            id_cliente = conn.execute(text(
                "INSERT INTO cliente (codigo_cti, id_consultor, nome_empresa, segmento, faturamento_anual, nivel,"
                " status, cidade, uf, lote_id)"
                " VALUES (:codigo, :consultor, :nome, :segmento, :fat, :nivel, 'ATIVO', :cidade, :uf, :lote)"
                " ON CONFLICT (codigo_cti) DO UPDATE SET id_consultor = EXCLUDED.id_consultor,"
                " nome_empresa = EXCLUDED.nome_empresa, segmento = EXCLUDED.segmento,"
                " faturamento_anual = EXCLUDED.faturamento_anual, nivel = EXCLUDED.nivel, status = 'ATIVO',"
                " cidade = EXCLUDED.cidade, uf = EXCLUDED.uf, lote_id = EXCLUDED.lote_id"
                " RETURNING id_cliente"),
                {"codigo": linha.codigo_cliente, "consultor": consultores[linha.consultor],
                 "nome": linha.nome_cliente[:180], "segmento": linha.segmento[:100], "fat": linha.faturamento_anual,
                 "nivel": linha.nivel_cliente, "cidade": linha.cidade, "uf": linha.uf, "lote": lote_id},
            ).scalar_one()
            for servico in linha.servicos:
                params = {"cliente": id_cliente, "servico": servicos[servico], "data": linha.data_contratacao, "lote": lote_id}
                atualizado = conn.execute(text(
                    "UPDATE contrato SET lote_id = :lote WHERE id_cliente = :cliente AND id_servico = :servico"
                    " AND data_inicio = CAST(:data AS DATE)"), params).rowcount
                if not atualizado:
                    conn.execute(text(
                        "INSERT INTO contrato (id_cliente, id_servico, data_inicio, status, lote_id)"
                        " VALUES (:cliente, :servico, CAST(:data AS DATE), 'ATIVO', :lote)"), params)
        conn.execute(text(
            "INSERT INTO estatistica_lote (lote_id, resultado, criado_em) VALUES (:lote, :json, now())"
            " ON CONFLICT (lote_id) DO UPDATE SET resultado = EXCLUDED.resultado, criado_em = now()"),
            {"lote": lote_id, "json": json.dumps(resultado, ensure_ascii=False, default=str)})
        conn.execute(text(
            "UPDATE upload_lote SET status = 'PROCESSADO', linhas_validas = :validas, linhas_rejeitadas_etl = :rej,"
            " mensagem = NULL, processado_em = now() WHERE id = :lote"),
            {"validas": int(len(df)), "rej": len(res.rejeitadas), "lote": lote_id})


def marcar_erro(engine, lote_id, mensagem):
    """Grava ERRO e o motivo no lote (a mensagem aparece na tela)."""
    with engine.begin() as conn:
        conn.execute(text("UPDATE upload_lote SET status = 'ERRO', mensagem = :msg, processado_em = now() WHERE id = :id"),
                     {"msg": str(mensagem)[:500], "id": lote_id})


def obter_lote(engine, lote_id):
    with engine.connect() as conn:
        linha = conn.execute(text("SELECT id, arquivo_nome, status, mensagem FROM upload_lote WHERE id = :id"),
                             {"id": lote_id}).mappings().first()
        return dict(linha) if linha else None


def listar_lotes(engine, limite=50):
    with engine.connect() as conn:
        linhas = conn.execute(text(
            "SELECT id, arquivo_nome, status, total_linhas, linhas_validas, mensagem, criado_em, processado_em"
            " FROM upload_lote ORDER BY id DESC LIMIT :limite"), {"limite": limite}).mappings().all()
        return [dict(linha) for linha in linhas]
