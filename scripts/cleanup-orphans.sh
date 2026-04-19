#!/bin/bash

set -euo pipefail

docker-compose down --remove-orphans
docker volume prune -f
