set -e

# Set password to avoid interactive prompt
# export PGPASSWORD="[PASSWORD]"

echo "PostgreSQL Backup and Restore Script"
echo "===================================="

# 1. CREATE BACKUP
# Use PostgreSQL 17 client (install if needed: sudo apt install postgresql-client-17)
BACKUP_FILE="backup_$(date +%Y%m%d_%H%M%S).sql"
echo "Creating a backup file: $BACKUP_FILE"
pg_dump -h dusanrychnovsky-postgres.postgres.database.azure.com \
        -U dusanrychnovsky \
        -d postgres \
        -f "$BACKUP_FILE"

echo "Backup completed: $BACKUP_FILE"

# 2. Clean the backup file to remove Azure-specific extensions and problematic statements
echo "Cleaning backup file for vanila Postgres compatibility..."
sed -e 's/CREATE EXTENSION IF NOT EXISTS pg_cron;//g' \
    -e 's/DROP EXTENSION IF EXISTS pg_cron;//g' \
    -e 's/COMMENT ON EXTENSION pg_cron.*;//g' \
    -e 's/CREATE EXTENSION IF NOT EXISTS azure;//g' \
    -e 's/DROP EXTENSION IF EXISTS azure;//g' \
    -e 's/CREATE EXTENSION IF NOT EXISTS pgaadauth;//g' \
    -e 's/DROP EXTENSION IF EXISTS pgaadauth;//g' \
    -e 's/COMMENT ON EXTENSION azure.*;//g' \
    -e 's/COMMENT ON EXTENSION pgaadauth.*;//g' \
    -e '/^CREATE EXTENSION.*pg_cron/d' \
    -e '/^DROP EXTENSION.*pg_cron/d' \
    -e '/^CREATE EXTENSION.*azure/d' \
    -e '/^DROP EXTENSION.*azure/d' \
    -e '/^CREATE EXTENSION.*pgaadauth/d' \
    -e '/^DROP EXTENSION.*pgaadauth/d' \
    -e '/^ALTER EXTENSION.*pg_cron/d' \
    -e '/^ALTER EXTENSION.*azure/d' \
    -e '/^ALTER EXTENSION.*pgaadauth/d' \
    -e '/^COMMENT ON EXTENSION.*pg_cron/d' \
    -e '/^COMMENT ON EXTENSION.*azure/d' \
    -e '/^COMMENT ON EXTENSION.*pgaadauth/d' \
    "$BACKUP_FILE" > "${BACKUP_FILE}.cleaned"

# 2b. Additional cleaning for stubborn Azure references
echo "Additional cleanup for Azure-specific content..."
sed -i \
    -e '/GRANT.*azure_pg_admin/d' \
    -e '/REVOKE.*azure_pg_admin/d' \
    -e '/OWNER TO.*azure_pg_admin/d' \
    -e 's/OWNER TO dusanrychnovsky/OWNER TO postgres/g' \
    "${BACKUP_FILE}.cleaned"

echo "Cleaned up backup file created: ${BACKUP_FILE}.cleaned"

# TESTING RESTORE - Local Docker Testing
# 3. Clean up any existing containers from previous testing
echo "Cleaning up any existing postgres-backup containers..."
docker stop postgres-backup 2>/dev/null || true
docker rm postgres-backup 2>/dev/null || true

echo "Starting a PostgreSQL container..."
docker run --name postgres-backup \
  -e POSTGRES_PASSWORD=testpassword \
  -e POSTGRES_DB=backup_postgres \
  -e POSTGRES_USER=postgres \
  -p 5433:5432 \
  -d postgres:17

# 4. Wait for container to be ready
sleep 10

# 5. Create the Azure user and roles to avoid role errors
echo "Creating Azure-compatible roles and users..."
docker exec postgres-backup psql -U postgres -d postgres -c "
CREATE ROLE dusanrychnovsky WITH LOGIN PASSWORD 'testpass' SUPERUSER;
CREATE ROLE azure_pg_admin;
GRANT azure_pg_admin TO dusanrychnovsky;
ALTER ROLE dusanrychnovsky CREATEDB CREATEROLE;
"

# 6. Restore the cleaned backup
docker exec -i postgres-backup psql -U postgres -d backup_postgres < "${BACKUP_FILE}.cleaned"

# 7. Verify the restore
echo "=== Sample data check ==="
docker exec postgres-backup psql -U postgres -d backup_postgres -c "SELECT title FROM myteacollection.Teas ORDER BY id DESC LIMIT 1"

# 8. Interactive testing
docker build --tag=my-tea-collection:latest .
docker run -p8080:8080 -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5433/backup_postgres -e SPRING_DATASOURCE_USERNAME=postgres -e SPRING_DATASOURCE_PASSWORD=testpassword my-tea-collection:latest

echo "=== To connect interactively: ==="
echo "SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/backup_postgres"
echo "SPRING_DATASOURCE_PASSWORD=testpassword"
echo "SPRING_DATASOURCE_USERNAME=postgres"
