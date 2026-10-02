#!/usr/bin/env bash
# ==============================================================================
# Distributed Computing Laboratory - Experiment 5
# Topic: Quorum-Based Split-Brain Preventing Leader Election
# ==============================================================================

set -e

# Detect Java installation
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" ]; then
        export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
    elif [ -d "/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home"
    elif [ -x "/usr/libexec/java_home" ]; then
        export JAVA_HOME="$(/usr/libexec/java_home 2>/dev/null || true)"
    fi
fi

if [ -n "$JAVA_HOME" ] && [ -d "$JAVA_HOME/bin" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

compile() {
    echo "================================================================="
    echo "Compiling Quorum Election Java Sources..."
    echo "================================================================="
    javac *.java
    echo "Compilation successful!"
}

run_sim() {
    compile
    echo ""
    echo "================================================================="
    echo "Launching Quorum Split-Brain Prevention Simulation Driver..."
    echo "================================================================="
    java SplitBrainSimDriver
}

run_interactive() {
    compile
    echo ""
    echo "================================================================="
    echo "Spawning Cluster Nodes for Interactive Console..."
    echo "================================================================="

    java QuorumElectionNode 1 none &
    PID1=$!
    java QuorumElectionNode 2 none &
    PID2=$!
    java QuorumElectionNode 3 none &
    PID3=$!
    java QuorumElectionNode 4 none &
    PID4=$!
    java QuorumElectionNode 5 none &
    PID5=$!

    cleanup() {
        echo ""
        echo "Terminating cluster nodes ($PID1 $PID2 $PID3 $PID4 $PID5)..."
        kill -9 $PID1 $PID2 $PID3 $PID4 $PID5 2>/dev/null || true
    }
    trap cleanup EXIT INT TERM

    sleep 2
    java SplitBrainInteractiveConsole
}

case "${1:-sim}" in
    compile)
        compile
        ;;
    sim|simulation)
        run_sim
        ;;
    interactive|cli)
        run_interactive
        ;;
    *)
        echo "Usage: $0 [sim|interactive|compile]"
        echo "  sim         - Run automated simulation harness (default)"
        echo "  interactive - Launch cluster and enter interactive CLI"
        echo "  compile     - Only compile Java source files"
        exit 1
        ;;
esac
