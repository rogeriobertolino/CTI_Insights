"""Cliente opcional: nunca grava diretamente no banco nem fixa credenciais."""
import os
from urllib.parse import urlparse
import requests

def enviar_resultados(importacao_id, analytics, api=None, token=None):
    api = (api or os.environ.get("CTI_API_URL", "http://127.0.0.1:8081")).rstrip("/")
    parsed = urlparse(api)
    if parsed.scheme != "https" and not (parsed.scheme == "http" and parsed.hostname in ("localhost", "127.0.0.1", "::1")):
        raise ValueError("Use HTTPS para enviar resultados fora da máquina local.")
    token = token or os.environ.get("CTI_API_TOKEN")
    if not token: raise ValueError("Defina CTI_API_TOKEN com um JWT de administrador.")
    response = requests.post(api + "/api/analises/resultado", json={"importacaoId": str(importacao_id), "estatisticas": analytics["statistics"]["overall"], "insights": analytics["insights"]}, headers={"Authorization": "Bearer " + token}, timeout=30, allow_redirects=False)
    if response.status_code >= 300: raise RuntimeError(f"API recusou o resultado: HTTP {response.status_code}.")
    return response.json()
