DON'T  use namespace xtdb.node. Conflict on namespace will introduce strange bug/issue

## Start local podman instance
     podman run --name pgsql -p 15432:5432 -e POSTGRES_PASSWORD=pwd -d docker.io/postgres:14-alpine

## Connect to local database
    psql -U postgres -p 15432 -h localhost

## Create test database

     CREATE USER xtdb WITH PASSWORD 'pwd';
     CREATE DATABASE xtdb_db ENCODING 'UTF8' OWNER xtdb;
     GRANT ALL PRIVILEGES ON DATABASE xtdb_db TO xtdb;


## Testing commands

    curl -X POST \
    -H "Content-Type: application/json" \
    -H "Accept: application/json" \
    -d '{"tx-ops": [
           ["put", {
                "xt/id": "demo-id",
                "name": "Joe",
                "last-name": "Unknown"
            }]
         ]}' \
     http://localhost:3000/_xtdb/submit-tx
     
     curl -X GET \
     -H "Accept: application/json" \
     http://localhost:3000/_xtdb/entity\?eid\=demo-id