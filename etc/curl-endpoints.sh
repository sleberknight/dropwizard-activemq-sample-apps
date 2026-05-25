#!/usr/bin/env bash
# Interactive curl-based client for the dropwizard-activemq sample apps.
# Useful in environments without IntelliJ, Bruno, or Postman (e.g. Codespaces).
#
# Usage: ./etc/curl-endpoints.sh

set -euo pipefail

PRODUCER_URL="http://localhost:8080"
CONSUMER_ALPHA_1_URL="http://localhost:8081"
CONSUMER_ALPHA_2_URL="http://localhost:8082"
CONSUMER_BETA_URL="http://localhost:8083"
PRODUCER_ADMIN_URL="http://localhost:8090"
CONSUMER_ALPHA_ADMIN_URL="http://localhost:8091"
CONSUMER_BETA_ADMIN_URL="http://localhost:8093"

# Colors
if [[ -t 1 ]]; then
    BOLD="\033[1m"
    CYAN="\033[0;36m"
    GREEN="\033[0;32m"
    YELLOW="\033[0;33m"
    MAGENTA="\033[0;35m"
    RESET="\033[0m"
else
    BOLD="" CYAN="" GREEN="" YELLOW="" MAGENTA="" RESET=""
fi

header()   { echo -e "\n${CYAN}==>${RESET} ${BOLD}$*${RESET}"; }
info()     { echo -e "${YELLOW}$*${RESET}"; }
section()  { echo -e "${BOLD}${GREEN}$*${RESET}"; }

show_menu() {
    echo ""
    echo -e "${BOLD}${MAGENTA}Dropwizard ActiveMQ Sample Apps — curl client${RESET}"
    echo ""
    section "Producer"
    echo "  1) POST /produce — send order to topic:orders (virtual topic)"
    echo "  2) POST /produce — send announcement to fixedtopic:announcements"
    echo "  3) POST /produce — send notification to queue:notifications"
    echo "  4) POST /produce — send to queue:poison_pill (triggers DLQ after 30s)"
    echo "  5) POST /produce — send multiple messages (count=5) to topic:orders"
    echo ""
    section "Consumer"
    echo "  6) GET  /received — consumer-alpha-1"
    echo "  7) GET  /received — consumer-alpha-2"
    echo "  8) GET  /received — consumer-beta"
    echo "  9) GET  /received?destination=topic:orders — consumer-alpha-1 filtered"
    echo " 10) DELETE /received — clear consumer-alpha-1 messages"
    echo " 11) DELETE /received — clear consumer-alpha-2 messages"
    echo " 12) DELETE /received — clear consumer-beta messages"
    echo " 13) DELETE /dlq — purge ActiveMQ.DLQ (resets dead-letter-queue health check)"
    echo ""
    section "Health checks"
    echo " 14) GET /healthcheck — producer"
    echo " 15) GET /healthcheck — consumer-alpha-1"
    echo " 16) GET /healthcheck — consumer-beta"
    echo ""
    echo "  q) Quit"
    echo ""
}

run_curl() {
    local description="$1"
    shift
    info "  ${description}"
    echo ""
    curl -s "$@" | jq .
    echo ""
}

while true; do
    show_menu
    read -r -p "Choose an option: " choice

    case "${choice}" in
        1)
            header "Send order to topic:orders (virtual topic)"
            run_curl "POST ${PRODUCER_URL}/produce" \
                -X POST "${PRODUCER_URL}/produce" \
                -H "Content-Type: application/json" \
                -d '{"destination":"topic:orders","sendToAllEventsQueue":true,"count":1,"format":"JSON_CURRENT","messageType":"ORDER_PLACED","data":{"orderId":"123","total":49.99}}'
            ;;
        2)
            header "Send announcement to fixedtopic:announcements"
            run_curl "POST ${PRODUCER_URL}/produce" \
                -X POST "${PRODUCER_URL}/produce" \
                -H "Content-Type: application/json" \
                -d '{"destination":"fixedtopic:announcements","sendToAllEventsQueue":false,"format":"JSON_CURRENT","messageType":"SYSTEM_NOTICE","data":{"message":"hello from curl client"}}'
            ;;
        3)
            header "Send notification to queue:notifications"
            run_curl "POST ${PRODUCER_URL}/produce" \
                -X POST "${PRODUCER_URL}/produce" \
                -H "Content-Type: application/json" \
                -d '{"destination":"queue:notifications","sendToAllEventsQueue":false,"count":1,"format":"JSON_CURRENT","messageType":"USER_NOTIFICATION","data":{"userId":"42","message":"your order shipped"}}'
            ;;
        4)
            header "Send poison pill to queue:poison_pill (expires after 30s -> DLQ)"
            run_curl "POST ${PRODUCER_URL}/produce" \
                -X POST "${PRODUCER_URL}/produce" \
                -H "Content-Type: application/json" \
                -d '{"destination":"queue:poison_pill","sendToAllEventsQueue":false,"format":"TEXT","data":"this will expire"}'
            info "  Wait 30s then check consumer-alpha health check (option 15) to see it go unhealthy."
            ;;
        5)
            header "Send 5 orders to topic:orders"
            run_curl "POST ${PRODUCER_URL}/produce" \
                -X POST "${PRODUCER_URL}/produce" \
                -H "Content-Type: application/json" \
                -d '{"destination":"topic:orders","sendToAllEventsQueue":true,"count":5,"format":"JSON_CURRENT","messageType":"ORDER_PLACED","data":{"orderId":"batch","total":9.99}}'
            ;;
        6)
            header "Received messages — consumer-alpha-1"
            run_curl "GET ${CONSUMER_ALPHA_1_URL}/received" "${CONSUMER_ALPHA_1_URL}/received"
            ;;
        7)
            header "Received messages — consumer-alpha-2"
            run_curl "GET ${CONSUMER_ALPHA_2_URL}/received" "${CONSUMER_ALPHA_2_URL}/received"
            ;;
        8)
            header "Received messages — consumer-beta"
            run_curl "GET ${CONSUMER_BETA_URL}/received" "${CONSUMER_BETA_URL}/received"
            ;;
        9)
            header "Received messages — consumer-alpha-1 filtered to topic:orders"
            run_curl "GET ${CONSUMER_ALPHA_1_URL}/received?destination=topic:orders" \
                "${CONSUMER_ALPHA_1_URL}/received?destination=topic:orders"
            ;;
        10)
            header "Clear received messages — consumer-alpha-1"
            run_curl "DELETE ${CONSUMER_ALPHA_1_URL}/received" \
                -X DELETE "${CONSUMER_ALPHA_1_URL}/received"
            ;;
        11)
            header "Clear received messages — consumer-alpha-2"
            run_curl "DELETE ${CONSUMER_ALPHA_2_URL}/received" \
                -X DELETE "${CONSUMER_ALPHA_2_URL}/received"
            ;;
        12)
            header "Clear received messages — consumer-beta"
            run_curl "DELETE ${CONSUMER_BETA_URL}/received" \
                -X DELETE "${CONSUMER_BETA_URL}/received"
            ;;
        13)
            header "Purge ActiveMQ.DLQ"
            run_curl "DELETE ${CONSUMER_ALPHA_1_URL}/dlq" \
                -X DELETE "${CONSUMER_ALPHA_1_URL}/dlq"
            ;;
        14)
            header "Health check — producer"
            run_curl "GET ${PRODUCER_ADMIN_URL}/healthcheck" "${PRODUCER_ADMIN_URL}/healthcheck"
            ;;
        15)
            header "Health check — consumer-alpha-1"
            run_curl "GET ${CONSUMER_ALPHA_ADMIN_URL}/healthcheck" "${CONSUMER_ALPHA_ADMIN_URL}/healthcheck"
            ;;
        16)
            header "Health check — consumer-beta"
            run_curl "GET ${CONSUMER_BETA_ADMIN_URL}/healthcheck" "${CONSUMER_BETA_ADMIN_URL}/healthcheck"
            ;;
        q|Q)
            echo ""
            echo "Exiting."
            exit 0
            ;;
        *)
            echo -e "${YELLOW}  Unknown option: ${choice}${RESET}"
            ;;
    esac
done
