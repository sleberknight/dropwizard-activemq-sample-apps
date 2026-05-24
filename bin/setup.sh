#!/usr/bin/env bash
# Clones and installs dropwizard-activemq locally, then builds the sample apps.
# Intended for use in environments without a local dropwizard-activemq checkout,
# such as GitHub Codespaces.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo "==> Cloning dropwizard-activemq..."
git clone https://github.com/kiwiproject/dropwizard-activemq.git /tmp/dropwizard-activemq

echo "==> Installing dropwizard-activemq to local Maven repository..."
mvn -f /tmp/dropwizard-activemq/pom.xml install -DskipTests

echo "==> Building sample apps..."
mvn -f "${PROJECT_DIR}/pom.xml" package -DskipTests

echo ""
echo "Setup complete. To start the services:"
echo "  cd ${PROJECT_DIR}/docker && docker compose up --build"
