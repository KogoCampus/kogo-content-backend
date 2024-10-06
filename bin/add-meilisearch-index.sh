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

    FILTERABLE_ATTRIBUTES='["id", "createdAt"]'
    SORTABLE_ATTRIBUTES='["id"]'

    if [ "$INDEX" = "topics" ]; then
            FILTERABLE_ATTRIBUTES='["id", "createdAt"]'
            SORTABLE_ATTRIBUTES='["id"]'
            SEARCHABLE_ATTRIBUTES='["topicName", "description", "tags"]'
        fi

    if [ "$INDEX" = "posts" ]; then
            FILTERABLE_ATTRIBUTES='["id", "createdAt"]'
            SORTABLE_ATTRIBUTES='["id"]'
            SEARCHABLE_ATTRIBUTES='["title", "content"]'
        fi

    if [ "$INDEX" = "comments" ]; then
            FILTERABLE_ATTRIBUTES='["id", "parentType", "parentCreatedAt", "createdAt"]'
            SORTABLE_ATTRIBUTES='["id", "parentId"]'
            SEARCHABLE_ATTRIBUTES='["content"]'
        fi

    echo "\n Setting filterable/sortable attributes for: $INDEX"
    curl -X PATCH "http://localhost:7700/indexes/$INDEX/settings" \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $MASTER_KEY" \
    --data-binary "{
        \"filterableAttributes\": $FILTERABLE_ATTRIBUTES,
        \"sortableAttributes\": $SORTABLE_ATTRIBUTES,
        \"searchableAttributes\": $SEARCHABLE_ATTRIBUTES
    }"

    echo "\n"
done

