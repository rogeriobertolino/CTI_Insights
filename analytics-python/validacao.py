"""Separação de registros válidos e inválidos, sem descartar suas causas."""
from collections import Counter
from limpeza import REQUIRED

def validar_dados(rows, originals=None):
    counts = Counter(str(row.get("codigo_cliente", "")).casefold() for row in rows)
    valid, invalid = [], []
    limits = {"codigo_cliente": 80, "cliente": 180, "consultor": 150, "segmento": 100, "servico": 150}
    for index, row in enumerate(rows):
        errors = []
        for field in REQUIRED:
            if row.get(field) is None or str(row.get(field, "")).strip() == "": errors.append(field + ": obrigatório ou inválido")
        if counts[str(row.get("codigo_cliente", "")).casefold()] > 1: errors.append("codigo_cliente: duplicado")
        if row.get("nivel") not in ("A", "B", "C"): errors.append("nivel: use A, B ou C")
        if row.get("status_contrato") not in ("ATIVO", "PENDENTE", "CANCELADO"): errors.append("status_contrato: inválido")
        for field in ("faturamento_mensal", "valor_servico"):
            if row.get(field) is None or row[field] < 0: errors.append(field + ": informe número não negativo")
        for field, limit in limits.items():
            if len(str(row.get(field, ""))) > limit: errors.append(field + ": tamanho excedido")
        target = invalid if errors else valid
        target.append({**row, **({"erros": errors, "original": originals[index] if originals else {}} if errors else {})})
    return valid, invalid
