#!/usr/bin/env bash
# =============================================================================
# run_coverage.sh — JaCoCo coverage for an externally-started Spring Boot server
#
# WHY this exists:
#   When tests hit a running server via REST/WebSocket, the JaCoCo Maven plugin
#   can't instrument the server JVM.  You must attach the JaCoCo runtime agent
#   to the *server* JVM at startup, then dump the .exec file after tests finish.
#
# WHAT this script does (fully automated, one command):
#   1. Download JaCoCo agent + CLI jars if not already cached by Maven
#   2. Build the project (skip tests — only need the JAR)
#   3. Start the Spring Boot server with JaCoCo agent attached (TCP server mode)
#   4. Wait until the server is ready (polls /actuator/health or port 8080)
#   5. Run the system tests via Maven Surefire
#   6. Dump the coverage data from the live agent over TCP
#   7. Stop the server
#   8. Merge + generate HTML report with the JaCoCo CLI
#   9. Open the report in your browser
#
# USAGE:
#   chmod +x run_coverage.sh
#   ./run_coverage.sh                          # uses default test class
#   ./run_coverage.sh MainSystemTest           # specify test class
#   ./run_coverage.sh MainSystemTest 8080      # specify test class + port
#
# REQUIREMENTS:
#   - Java 17+, Maven 3.x on PATH
#   - MySQL running (same as normal dev)
#   - Internet access to download JaCoCo jars on first run (cached afterwards)
# =============================================================================

set -euo pipefail

# ── Configuration ─────────────────────────────────────────────────────────────
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
TEST_CLASS="${1:-MainSystemTest}"
SERVER_PORT="${2:-8080}"
JACOCO_VERSION="0.8.11"
JACOCO_TCP_PORT="6300"                          # agent listens here for dump commands

# Maven local repo — resolves ~ correctly even inside scripts
M2_REPO="${HOME}/.m2/repository"
AGENT_JAR="${M2_REPO}/org/jacoco/org.jacoco.agent/${JACOCO_VERSION}/org.jacoco.agent-${JACOCO_VERSION}-runtime.jar"
CLI_JAR="${M2_REPO}/org/jacoco/org.jacoco.cli/${JACOCO_VERSION}/org.jacoco.cli-${JACOCO_VERSION}-nodeps.jar"

EXEC_FILE="${PROJECT_DIR}/target/jacoco-server.exec"
CLASSES_DIR="${PROJECT_DIR}/target/classes"
SOURCES_DIR="${PROJECT_DIR}/src/main/java"
REPORT_DIR="${PROJECT_DIR}/target/site/jacoco"
SERVER_LOG="${PROJECT_DIR}/target/server.log"

# ── Colours ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
info()    { echo -e "${BLUE}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
die()     { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }

# ── Cleanup on exit ───────────────────────────────────────────────────────────
SERVER_PID=""
cleanup() {
    if [[ -n "$SERVER_PID" ]] && kill -0 "$SERVER_PID" 2>/dev/null; then
        info "Stopping server (PID $SERVER_PID)..."
        kill "$SERVER_PID" 2>/dev/null || true
        wait "$SERVER_PID" 2>/dev/null || true
        success "Server stopped."
    fi
}
trap cleanup EXIT INT TERM

# ── Step 1: Download JaCoCo jars via Maven if not cached ─────────────────────
download_jacoco_jar() {
    local GROUP_PATH="org/jacoco"
    local ARTIFACT="$1"          # e.g. org.jacoco.agent
    local CLASSIFIER="$2"        # e.g. runtime  or  nodeps
    local TARGET_JAR="$3"

    if [[ -f "$TARGET_JAR" ]]; then
        info "JaCoCo $(basename "$TARGET_JAR") already cached — skipping download."
        return 0
    fi

    info "Downloading JaCoCo ${ARTIFACT}:${JACOCO_VERSION}:${CLASSIFIER} ..."
    mvn --quiet dependency:get \
        -Dartifact="org.jacoco:${ARTIFACT}:${JACOCO_VERSION}:jar:${CLASSIFIER}" \
        -Ddest="$TARGET_JAR" 2>/dev/null || \
    mvn --quiet dependency:copy \
        -Dartifact="org.jacoco:${ARTIFACT}:${JACOCO_VERSION}:jar:${CLASSIFIER}" \
        -DoutputDirectory="$(dirname "$TARGET_JAR")" 2>/dev/null || true

    # Fallback: download directly with Maven get plugin
    if [[ ! -f "$TARGET_JAR" ]]; then
        mvn --quiet org.apache.maven.plugins:maven-dependency-plugin:3.6.0:get \
            -Dartifact="org.jacoco:${ARTIFACT}:${JACOCO_VERSION}:jar:${CLASSIFIER}" \
            -Ddest="$TARGET_JAR" 2>/dev/null || true
    fi

    [[ -f "$TARGET_JAR" ]] || die "Failed to download $(basename "$TARGET_JAR"). Run manually:\n  mvn dependency:get -Dartifact=org.jacoco:${ARTIFACT}:${JACOCO_VERSION}:jar:${CLASSIFIER}"
    success "Downloaded $(basename "$TARGET_JAR")"
}

cd "$PROJECT_DIR"

info "=== JaCoCo Coverage Runner ==="
info "Project : $PROJECT_DIR"
info "Test    : $TEST_CLASS"
info "Port    : $SERVER_PORT"
echo ""

# Ensure target/ exists
mkdir -p target

# Download agent jar
download_jacoco_jar "org.jacoco.agent" "runtime" "$AGENT_JAR"
# Download CLI jar (for report generation — avoids needing jacoco plugin in pom.xml)
download_jacoco_jar "org.jacoco.cli"   "nodeps"  "$CLI_JAR"

# ── Step 2: Build (compile + package, skip tests) ─────────────────────────────
info "Building project (mvn package -DskipTests)..."
mvn --quiet package -DskipTests || die "Build failed — fix compilation errors first."
success "Build complete."
echo ""

# Find the built JAR
SERVER_JAR=$(find target -maxdepth 1 -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" | sort | tail -1)
[[ -f "$SERVER_JAR" ]] || die "No JAR found in target/. Did the build succeed?"
info "Server JAR: $SERVER_JAR"

# ── Step 3: Start server with JaCoCo agent ────────────────────────────────────
# Agent options:
#   output=tcpserver  → agent listens on TCP so we can dump coverage without stopping JVM
#   address=localhost → only accept local connections
#   port=6300         → TCP port for the dump command
#   destfile=...      → fallback if agent never dumped (e.g. on normal exit)
AGENT_OPTS="output=tcpserver,address=localhost,port=${JACOCO_TCP_PORT},destfile=${EXEC_FILE}"

info "Starting server with JaCoCo agent (TCP port ${JACOCO_TCP_PORT})..."
rm -f "$EXEC_FILE"
java \
    -javaagent:"${AGENT_JAR}=${AGENT_OPTS}" \
    -jar "$SERVER_JAR" \
    --server.port="$SERVER_PORT" \
    > "$SERVER_LOG" 2>&1 &
SERVER_PID=$!
info "Server PID: $SERVER_PID  (log: $SERVER_LOG)"

# ── Step 4: Wait for server to be ready ───────────────────────────────────────
info "Waiting for server on port ${SERVER_PORT}..."
MAX_WAIT=60
ELAPSED=0
while ! curl -sf "http://localhost:${SERVER_PORT}/actuator/health" >/dev/null 2>&1 && \
      ! curl -sf "http://localhost:${SERVER_PORT}/swagger-ui/index.html" >/dev/null 2>&1 && \
      ! nc -z localhost "$SERVER_PORT" 2>/dev/null; do
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
        echo ""
        die "Server process died. Check $SERVER_LOG for errors."
    fi
    if (( ELAPSED >= MAX_WAIT )); then
        echo ""
        die "Server did not start within ${MAX_WAIT}s. Check $SERVER_LOG."
    fi
    printf "."
    sleep 2
    (( ELAPSED += 2 )) || true
done
echo ""
success "Server is up (${ELAPSED}s)."
echo ""

# Give it one extra second to finish wiring up WebSocket endpoints
sleep 1

# ── Step 5: Run the system tests ──────────────────────────────────────────────
info "Running tests: ${TEST_CLASS}..."
echo ""
# -fae = fail-at-end (run all tests even if some fail, so we still get coverage)
TEST_EXIT=0
mvn --no-transfer-progress surefire:test \
    -Dtest="${TEST_CLASS}" \
    -DfailIfNoTests=false \
    -Dfailsafe.failIfNoSpecifiedTests=false \
    -fae || TEST_EXIT=$?
echo ""

# ── Step 6: Dump coverage data from the live agent ───────────────────────────
info "Dumping coverage data from JaCoCo agent (TCP port ${JACOCO_TCP_PORT})..."
# The CLI dump command connects to the TCP server, requests a dump, and writes the .exec file
java -jar "$CLI_JAR" dump \
    --address localhost \
    --port "$JACOCO_TCP_PORT" \
    --destfile "$EXEC_FILE" \
    --reset || warn "TCP dump failed — will fall back to destfile written on server exit."

if [[ -f "$EXEC_FILE" ]]; then
    success "Coverage data saved to: $EXEC_FILE"
else
    warn "No .exec file yet — it will be written when the server stops."
fi

# ── Step 7: Stop the server (triggers destfile write as fallback) ─────────────
info "Stopping server..."
kill "$SERVER_PID" 2>/dev/null || true
wait "$SERVER_PID" 2>/dev/null || true
SERVER_PID=""   # prevent double-kill in trap
sleep 1
success "Server stopped."

# Wait for exec file (written on JVM exit if TCP dump wasn't used)
if [[ ! -f "$EXEC_FILE" ]]; then
    warn "Waiting for .exec file to be flushed to disk..."
    sleep 2
fi
[[ -f "$EXEC_FILE" ]] || die "No jacoco.exec file found at $EXEC_FILE — coverage data was not captured."
success "Exec file size: $(du -h "$EXEC_FILE" | cut -f1)"
echo ""

# ── Step 8: Generate HTML report ─────────────────────────────────────────────
info "Generating HTML report..."
rm -rf "$REPORT_DIR"
mkdir -p "$REPORT_DIR"

java -jar "$CLI_JAR" report "$EXEC_FILE" \
    --classfiles  "$CLASSES_DIR" \
    --sourcefiles "$SOURCES_DIR" \
    --html        "$REPORT_DIR" \
    --xml         "${REPORT_DIR}/jacoco.xml" \
    --csv         "${REPORT_DIR}/jacoco.csv" \
    --name        "System Test Coverage"

success "Report written to: ${REPORT_DIR}/index.html"
echo ""

# ── Step 9: Print summary and open report ────────────────────────────────────
# Quick text summary from the CSV
if [[ -f "${REPORT_DIR}/jacoco.csv" ]]; then
    echo "── Coverage Summary ──────────────────────────────────────────────────────"
    # CSV columns: GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,...
    awk -F',' 'NR>1 {
        im+=$4; ic+=$5;   # instructions
        bm+=$6; bc+=$7;   # branches
        lm+=$10; lc+=$11  # lines
    }
    END {
        total_i = im+ic; total_b = bm+bc; total_l = lm+lc
        printf "  Instructions : %d/%d  (%.0f%%)\n", ic, total_i, (total_i>0 ? ic/total_i*100 : 0)
        printf "  Branches     : %d/%d  (%.0f%%)\n", bc, total_b, (total_b>0 ? bc/total_b*100 : 0)
        printf "  Lines        : %d/%d  (%.0f%%)\n", lc, total_l, (total_l>0 ? lc/total_l*100 : 0)
    }' "${REPORT_DIR}/jacoco.csv"
    echo "──────────────────────────────────────────────────────────────────────────"
    echo ""
fi

# Open report in browser (works on macOS and Linux)
REPORT_URL="file://${REPORT_DIR}/index.html"
if command -v open >/dev/null 2>&1; then
    open "$REPORT_URL"
elif command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$REPORT_URL"
else
    info "Open manually: $REPORT_URL"
fi

# Exit with test result so CI can catch failures
if (( TEST_EXIT != 0 )); then
    warn "Some tests failed (exit code ${TEST_EXIT}) — but coverage report was still generated."
    exit "$TEST_EXIT"
fi

success "All done!"