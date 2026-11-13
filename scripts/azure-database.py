"""Inspeciona ou aplica SQL no Azure; credenciais ficam somente no ambiente do psql."""
import argparse
import json
import os
from datetime import datetime, timezone
from pathlib import Path
import subprocess

from dotenv import dotenv_values

ROOT = Path(__file__).resolve().parents[1]


def settings():
    values = dotenv_values(ROOT / 'analytics-python/.env')
    return {key: values.get(key, '') for key in ('DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_USER', 'DB_PASSWORD', 'DB_SSLMODE')}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--settings-json', action='store_true', help='Uso interno pelo inicializador; inclui credenciais.')
    parser.add_argument('--sql', type=Path)
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    values = settings()
    if args.settings_json:
        print(json.dumps(values))
        return
    if not values['DB_HOST'].endswith('.postgres.database.azure.com') or values['DB_SSLMODE'] not in ('require', 'verify-full'):
        raise SystemExit('Configure o host Azure e SSL no analytics-python/.env.')
    config = json.loads((Path(os.environ['LOCALAPPDATA']) / 'CTI_Insights/config.json').read_text(encoding='utf-8-sig'))
    psql = str(Path(config['PostgresBin']) / 'psql.exe')
    env = os.environ.copy()
    for pg, db in [('PGHOST', 'DB_HOST'), ('PGPORT', 'DB_PORT'), ('PGDATABASE', 'DB_NAME'), ('PGUSER', 'DB_USER'), ('PGPASSWORD', 'DB_PASSWORD'), ('PGSSLMODE', 'DB_SSLMODE')]:
        env[pg] = values[db]
    env['PGCONNECT_TIMEOUT'] = '15'
    env['PGCLIENTENCODING'] = 'UTF8'
    env['PGOPTIONS'] = '-c statement_timeout=30000 -c lock_timeout=5000'
    if not args.apply:
        env['PGOPTIONS'] += ' -c default_transaction_read_only=on'

    def execute(sql):
        result = subprocess.run([psql, '-X', '-w', '-v', 'ON_ERROR_STOP=1', '-At', '-c', sql], env=env, capture_output=True, encoding='utf-8', timeout=90)
        if result.returncode:
            raise RuntimeError(result.stderr.replace(values['DB_PASSWORD'], '[oculta]') if values['DB_PASSWORD'] else result.stderr)
        return result.stdout

    if args.apply:
        if not args.sql:
            raise SystemExit('--apply exige --sql.')
        tables = ['cliente', 'consultor', 'contratos', 'servicos', 'insights', 'telemetrias']
        backup = {'host': values['DB_HOST'], 'database': values['DB_NAME'], 'tables': {}}
        backup['columns'] = json.loads(execute("SELECT coalesce(json_agg(c),'[]') FROM information_schema.columns c WHERE table_schema='public'"))
        backup['constraints'] = json.loads(execute("SELECT coalesce(json_agg(c),'[]') FROM (SELECT conrelid::regclass::text AS table_name, conname, pg_get_constraintdef(oid) AS definition FROM pg_constraint WHERE connamespace='public'::regnamespace) c"))
        for table in tables:
            backup['tables'][table] = json.loads(execute(f'SELECT coalesce(json_agg(t),\'[]\') FROM public.{table} t'))
        directory = ROOT / '.runtime/backups'
        directory.mkdir(parents=True, exist_ok=True)
        path = directory / ('azure-before-' + datetime.now(timezone.utc).strftime('%Y%m%d-%H%M%S') + '.json')
        path.write_text(json.dumps(backup, ensure_ascii=False, indent=2), encoding='utf-8')
        print('Snapshot de dados e metadados salvo em', path)
        sql = args.sql.read_text(encoding='utf-8-sig')
        print(execute('BEGIN;\n' + sql + '\nCOMMIT;'))
        print('Migracao aplicada em transacao.')
    elif args.sql:
        print(execute(args.sql.read_text(encoding='utf-8-sig')))
    else:
        print(execute('SELECT current_database(),current_user; SELECT tablename FROM pg_tables WHERE schemaname=\'public\' ORDER BY tablename;'))


if __name__ == '__main__':
    main()
