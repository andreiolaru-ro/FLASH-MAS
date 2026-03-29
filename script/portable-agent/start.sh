#!/bin/bash

# Check for the bundled JRE
if [ -d "./jre" ] && [ -f "./jre/bin/java" ]; then
    echo "Found bundled JRE."
    JAVA_EXE="./jre/bin/java"
else
    echo "Bundled JRE not found. Using system's java."
    JAVA_EXE="java"
fi

echo "Starting Flash-MAS Daemon..."
# The "$@" passes all command-line arguments to the daemon
"$JAVA_EXE" -jar FlashMasDaemon.jar "$@"