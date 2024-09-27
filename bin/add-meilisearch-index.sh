#!/bin/sh

MASTER_KEY=$1

# Define the indexes you want to create
INDEXES=("topics" "posts")

# Loop through each index and create it using Meilisearch API
for INDEX in "${INDEXES[@]}"
do
    echo "Creating index: $INDEX"

    curl -X POST 'http://localhost:7700/indexes' \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $MASTER_KEY" \
    --data-binary "{
      \"uid\": \"$INDEX\",
      \"primaryKey\": \"id\"
    }"

    echo "Verifying index creation for: $INDEX" \
    curl -X GET "http://localhost:7700/indexes/$INDEX" \
    -H "Authorization: Bearer $MASTER_KEY"
    echo "\n"
done

