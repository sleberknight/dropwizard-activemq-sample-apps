#!/usr/bin/env bash
# Builds the sample apps.
#
# Optionally clones and locally installs dropwizard-activemq for development
# or feature exploration (e.g. in GitHub Codespaces or a similar environment).
# If you choose to clone, you will also be offered the option to update the
# dropwizard-activemq version in the sample apps POM to the cloned SNAPSHOT
# version. Revert that change before committing.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
DEFAULT_CLONE_DIR="/tmp"

# Colors (disabled automatically if not a terminal)
if [[ -t 1 ]]; then
    BOLD="\033[1m"
    CYAN="\033[0;36m"
    GREEN="\033[0;32m"
    YELLOW="\033[0;33m"
    RESET="\033[0m"
else
    BOLD="" CYAN="" GREEN="" YELLOW="" RESET=""
fi

info()    { echo -e "${CYAN}==>${RESET} ${BOLD}$*${RESET}"; }
success() { echo -e "${GREEN}==>${RESET} ${BOLD}$*${RESET}"; }
warn()    { echo -e "${YELLOW}  WARNING:${RESET} $*"; }
note()    { echo -e "${YELLOW}  NOTE:${RESET} $*"; }

echo -e "${BOLD}This script builds the sample apps.${RESET} It can also optionally clone and"
echo "locally install dropwizard-activemq, which is useful if you want to"
echo "make or test local changes to it (e.g. in GitHub Codespaces)."
echo ""

read -r -p "Clone and locally install dropwizard-activemq for development/testing? [y/N/q (quit)] " clone_response
clone_response="${clone_response:-N}"

if [[ "${clone_response}" =~ ^[Qq]$ ]]; then
    echo ""
    echo "Exiting."
    exit 0
fi

if [[ "${clone_response}" =~ ^[Yy]$ ]]; then
    read -r -p "Clone into directory [${DEFAULT_CLONE_DIR}]: " clone_parent
    clone_parent="${clone_parent:-${DEFAULT_CLONE_DIR}}"
    CLONE_DIR="${clone_parent}/dropwizard-activemq"

    info "Cloning dropwizard-activemq into ${CLONE_DIR}..."
    git clone https://github.com/kiwiproject/dropwizard-activemq.git "${CLONE_DIR}"

    info "Installing dropwizard-activemq to local Maven repository..."
    mvn -f "${CLONE_DIR}/pom.xml" install -DskipTests

    SNAPSHOT_VERSION=$(mvn -f "${CLONE_DIR}/pom.xml" help:evaluate \
        -Dexpression=project.version -q -DforceStdout)

    echo ""
    read -r -p "Update sample apps to use dropwizard-activemq ${SNAPSHOT_VERSION}? [Y/n] " update_response
    update_response="${update_response:-Y}"

    if [[ "${update_response}" =~ ^[Yy]$ ]]; then
        info "Updating dropwizard-activemq.version to ${SNAPSHOT_VERSION}..."
        mvn -f "${PROJECT_DIR}/pom.xml" versions:set-property \
            -Dproperty=dropwizard-activemq.version \
            -DnewVersion="${SNAPSHOT_VERSION}" \
            -DgenerateBackupPoms=false
        note "Revert this change before committing."
    else
        echo ""
        warn "The sample apps will NOT pick up local changes to dropwizard-activemq."
        echo "           They will use the released version declared in the sample apps POM."
        echo "           To use your local build, manually update the dropwizard-activemq.version"
        echo "           property in ${PROJECT_DIR}/pom.xml to ${SNAPSHOT_VERSION}."
    fi

    echo ""
fi

info "Building sample apps..."
mvn -f "${PROJECT_DIR}/pom.xml" package -DskipTests

echo ""
success "Setup complete. To start the services:"
echo "  cd ${PROJECT_DIR}/docker && docker compose up --build"
