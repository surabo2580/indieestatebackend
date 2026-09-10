#!/usr/bin/env bash
set -euo pipefail

# Creates the local Homebrew Postgres role and database used by application.yml.
# Does not require Docker. Re-run safely if they already exist.

psql -d postgres -v ON_ERROR_STOP=1 <<'SQL'
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'indieestate') THEN
        CREATE ROLE indieestate WITH LOGIN PASSWORD 'indieestate';
    END IF;
END
$$;

SELECT 'CREATE DATABASE indieestate OWNER indieestate'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'indieestate')\gexec

GRANT ALL PRIVILEGES ON DATABASE indieestate TO indieestate;
SQL

psql -d indieestate -v ON_ERROR_STOP=1 <<'SQL'
GRANT ALL ON SCHEMA public TO indieestate;
ALTER SCHEMA public OWNER TO indieestate;
SQL

echo "Local database ready: postgresql://indieestate:indieestate@localhost:5432/indieestate"
