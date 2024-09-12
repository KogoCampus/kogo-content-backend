#!/bin/sh

echo "Running meilisearch container...\n"
if [[ ! $(which docker) && $(docker --version) ]]; then
    echo "Error! Docker is not available.\n"
    echo "check if 'docker ps' command is available."
    exit 1
fi

echo "Pulling Meilisearch image\n"
docker pull getmeili/meilisearch:latest

docker run -it --rm \
  -p 7700:7700 \
  -e MEILI_MASTER_KEY='MASTER_KEY'\
  -v $(pwd)/meili_data:/meili_data \
  getmeili/meilisearch:v1.10