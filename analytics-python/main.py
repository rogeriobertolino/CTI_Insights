"""Coordena o ETL de um lote: reservar, tratar, calcular e gravar."""
import logging

import pandas as pd

from analise import calcular
from banco import gravar_resultado, marcar_erro, reservar_lote
from limpeza import tratar

LOG = logging.getLogger("cti.etl")


class LoteIndisponivel(Exception):
    """O lote não existe ou já foi processado por outro pedido."""


def processar_lote(engine, lote_id, linhas, recebidas, invalidas=0, com_aviso=0):
    """Roda as 4 etapas. Devolve True se gravou; em caso de erro marca ERRO e devolve False."""
    if not reservar_lote(engine, lote_id):              # 1. PENDENTE -> PROCESSANDO
        raise LoteIndisponivel(f"O lote {lote_id} não existe ou não está pendente.")
    try:
        res = tratar(pd.DataFrame(linhas))              # 2. limpeza.py
        if res.tratado.empty:
            raise ValueError("Nenhuma linha válida depois da limpeza.")
        resultado = calcular(res, recebidas, invalidas, com_aviso)  # 3. analise.py + insights.py
        gravar_resultado(engine, lote_id, res, resultado)           # 4. banco.py -> PROCESSADO
        LOG.info("Lote %s processado: %s clientes, %s rejeitadas.", lote_id, len(res.tratado), len(res.rejeitadas))
        return True
    except Exception as erro:
        LOG.exception("Falha no ETL do lote %s", lote_id)
        marcar_erro(engine, lote_id, f"{erro.__class__.__name__}: {erro}")
        return False
