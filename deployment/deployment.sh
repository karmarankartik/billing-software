#!/bin/bash

set -e

SHA="${1:-}"

if [[ -z "$SHA" ]]; then
    echo "Usage: ./deployment.sh <git-sha>"
    exit 1
fi

echo "=========================================="
echo "Deploying billing-software"
echo "Commit: $SHA"
echo "=========================================="

COMPOSE_FILE="/tmp/billing-deployment/docker-compose.yml"
ENV_FILE="/tmp/billing-secrets.env"

# Pull the exact image built by CI for this commit
docker pull "ghcr.io/karmarankartik/billing-software:${SHA}"

# Stop and remove the current stack.
# No -v: PostgreSQL volume is preserved.
IMAGE_TAG="$SHA" \
docker compose \
    --env-file "$ENV_FILE" \
    -f "$COMPOSE_FILE" \
    down

# Start the stack using the exact SHA.
IMAGE_TAG="$SHA" \
docker compose \
    --env-file "$ENV_FILE" \
    -f "$COMPOSE_FILE" \
    up -d

echo ""
echo "Checking application health..."

for ATTEMPT in {1..12}; do

    echo "Health check $ATTEMPT/3..."

    if curl --fail --silent --show-error \
        --max-time 5 \
        http://127.0.0.1:8080/actuator/health \
        > /dev/null; then

        echo "Application is healthy."
        echo "=========================================="
        echo "DEPLOYMENT SUCCESSFUL"
        echo "=========================================="
        exit 0
    fi

    echo "Health check failed."

    if [[ "$ATTEMPT" -lt 3 ]]; then
        sleep 5
    fi

done

echo ""
echo "=========================================="
echo "DEPLOYMENT FAILED"
echo "=========================================="
echo "Application failed all 3 health checks."

exit 1
