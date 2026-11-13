"""Confere se o Python alcança o banco do .env. Deve aparecer "Conectou!"."""
import sys

from sqlalchemy import text

from banco import criar_engine

try:
    with criar_engine().connect() as conn:
        versao = conn.execute(text("SELECT version()")).scalar_one()
    print("Conectou!")
    print(versao.split(",")[0])
except Exception as erro:
    print(f"Não conectou: {erro.__class__.__name__}: {erro}")
    print("Confira DB_HOST, DB_NAME, DB_USER, DB_PASSWORD e DB_SSLMODE no analytics-python/.env.")
    sys.exit(1)
