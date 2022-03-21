DON'T  use namespace xtdb.node. Conflict on namespace will introduce strange bug/issue

* Testing commands

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