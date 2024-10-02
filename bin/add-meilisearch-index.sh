#!/bin/sh

MASTER_KEY=$1

# Define the indexes you want to create
INDEXES=("topics" "posts" "comments")

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

    echo "\n Verifying index creation for: $INDEX" \
    curl -X GET "http://localhost:7700/indexes/$INDEX" \
    -H "Authorization: Bearer $MASTER_KEY"

    # Common filterable attributes, including 'id' for all indexes
    FILTERABLE_ATTRIBUTES='["id"]'

    if [ "$INDEX" = "comments" ]; then
        # Add 'parentType' as filterable for 'comments' index
        FILTERABLE_ATTRIBUTES='["id", "parentType"]'
    fi

    echo "\n Setting filterable attributes for: $INDEX"
    curl -X PATCH "http://localhost:7700/indexes/$INDEX/settings" \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $MASTER_KEY" \
    --data-binary "{
        \"filterableAttributes\": $FILTERABLE_ATTRIBUTES
    }"

    echo "\n"
done

