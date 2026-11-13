"""Lê o .env e monta o endereço do banco (local, Neon ou Azure: só o .env muda)."""
import os
from pathlib import Path

from dotenv import load_dotenv

# O .env fica ao lado deste arquivo e nunca vai para o Git.
load_dotenv(Path(__file__).resolve().parent / ".env")


def database_url():
    """Endereço SQLAlchemy do PostgreSQL. DATABASE_URL completo tem prioridade."""
    from sqlalchemy.engine import URL

    pronto = os.getenv("DATABASE_URL", "").strip()
    if pronto:
        return pronto.replace("postgresql://", "postgresql+psycopg://", 1) if pronto.startswith("postgresql://") else pronto
    return URL.create(
        "postgresql+psycopg",
        username=os.getenv("DB_USER", "cti"),
        password=os.getenv("DB_PASSWORD") or None,
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "5432")),
        database=os.getenv("DB_NAME", "cti_insights"),
        query={"sslmode": os.getenv("DB_SSLMODE", "prefer")},
    )


def etl_api_key():
    """Chave combinada com o Spring (cabeçalho X-ETL-Key). Vazia = sem chave."""
    return os.getenv("ETL_API_KEY", "").strip()
