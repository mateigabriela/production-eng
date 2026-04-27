#!/usr/bin/env bash
set -xeuo pipefail

# Stop and remove the compose stacks for mongo and prod-eng service profiles
docker compose --profile mongo --profile prod-eng-service down || true

exit 0
