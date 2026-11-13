"""A porta de entrada do Python: o Spring chama POST /etl.

Ligue com:  uvicorn api:app --port 8000      Teste em:  http://localhost:8000/docs
"""
import hmac
import logging
from functools import lru_cache
from typing import Any

import pandas as pd
from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel, Field

from analise import calcular
from banco import criar_engine, criar_lote, garantir_tabelas, listar_lotes, obter_lote
from config import etl_api_key
from limpeza import tratar
from main import LoteIndisponivel, processar_lote

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")

app = FastAPI(title="CTI Insights — ETL em Python")


class Pedido(BaseModel):
    """O "molde" do JSON enviado pelo PythonEtlService.java (nomes iguais ao pedido.put do Java)."""
    loteId: int | None = Field(None, gt=0)
    arquivoNome: str = Field("planilha", max_length=255)
    recebidas: int = Field(ge=0, le=10000)
    invalidas: int = Field(0, ge=0)
    comAviso: int = Field(0, ge=0)
    enviadoPor: str | None = Field(None, max_length=254)
    linhas: list[dict[str, Any]] = Field(min_length=1, max_length=10000)


@lru_cache(maxsize=1)
def obter_engine():
    """Conecta uma vez no banco do .env (local, Neon ou Azure) e garante as tabelas do ETL."""
    engine = criar_engine()
    garantir_tabelas(engine)
    return engine


def conferir_chave(x_etl_key):
    chave = etl_api_key()
    if chave and not hmac.compare_digest((x_etl_key or "").encode(), chave.encode()):
        raise HTTPException(401, "Chave inválida.")


@app.get("/saude")
def saude() -> dict:
    """O Spring pergunta "você está ligado?" antes de mandar o lote."""
    return {"status": "ok"}


@app.post("/etl")
def etl(pedido: Pedido, x_etl_key: str | None = Header(default=None)):
    """Recebe as linhas válidas, limpa, calcula e grava no banco."""
    conferir_chave(x_etl_key)
    try:
        engine = obter_engine()
        lote_id = pedido.loteId or criar_lote(engine, pedido.arquivoNome, pedido.recebidas,
                                              pedido.invalidas, pedido.comAviso, pedido.enviadoPor)
        ok = processar_lote(engine, lote_id, pedido.linhas, pedido.recebidas, pedido.invalidas, pedido.comAviso)
    except LoteIndisponivel as erro:
        raise HTTPException(409, str(erro)) from erro
    except Exception as erro:
        raise HTTPException(500, f"Falha no ETL: {erro.__class__.__name__}: {erro}") from erro
    if not ok:
        lote = obter_lote(engine, lote_id) or {}
        raise HTTPException(500, f"Falha no ETL: {lote.get('mensagem') or 'erro desconhecido'}")
    return {"ok": True, "loteId": lote_id}


@app.post("/testar")
def testar(linhas: list[dict[str, Any]]):
    """Só limpa e calcula, sem gravar nada. Cole o amostras/clientes.json em /docs."""
    if not linhas:
        raise HTTPException(422, "Envie ao menos uma linha.")
    res = tratar(pd.DataFrame(linhas))
    resultado = calcular(res, len(linhas))
    return {**resultado, "linhasTratadas": res.tratado.to_dict(orient="records"),
            "rejeitadas": res.rejeitadas, "avisos": res.avisos,
            "resultado": resultado}


@app.get("/lotes")
def lotes(x_etl_key: str | None = Header(default=None)):
    """Desafio da aula: ver os lotes gravados sem abrir o DBeaver."""
    conferir_chave(x_etl_key)
    return listar_lotes(obter_engine())
