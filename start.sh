#!/usr/bin/env bash
set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
MODE=${1:-same}
PORT=${2:-5000}
if (( $# > 2 )); then
    echo "Usage: bash start.sh {same|separate|server|client} [port]" >&2
    exit 2
fi
case "$MODE" in
    same|separate|server|client) ;;
    *) echo "Unknown mode: $MODE" >&2; exit 2 ;;
esac
if [[ "$MODE" == same && $# -gt 1 ]]; then
    echo "same mode does not take a port" >&2
    exit 2
fi
if [[ ! "$PORT" =~ ^[0-9]{1,5}$ ]] || (( 10#$PORT < 1 || 10#$PORT > 65535 )); then
    echo "Port must be between 1 and 65535" >&2
    exit 2
fi

mvn -q -f "$ROOT/pom.xml" compile
JAVA=(java -cp "$ROOT/target/classes" com.example.players.Main)

if [[ "$MODE" == same ]]; then
    exec "${JAVA[@]}" same
elif [[ "$MODE" != separate ]]; then
    exec "${JAVA[@]}" "$MODE" "$PORT"
fi

SERVER_PID=
CLIENT_PID=
cleanup() {
    local pid
    for pid in "$CLIENT_PID" "$SERVER_PID"; do
        if [[ -n "$pid" ]]; then
            kill "$pid" 2>/dev/null || true
            wait "$pid" 2>/dev/null || true
        fi
    done
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

"${JAVA[@]}" server "$PORT" &
SERVER_PID=$!
"${JAVA[@]}" client "$PORT" &
CLIENT_PID=$!

# The client retries refused connections while the server is starting.
STATUS=0
wait "$CLIENT_PID" || STATUS=$?
CLIENT_PID=
if (( STATUS != 0 )); then
    exit "$STATUS"
fi
wait "$SERVER_PID" || STATUS=$?
SERVER_PID=
exit "$STATUS"
