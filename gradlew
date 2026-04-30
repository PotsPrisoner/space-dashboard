#!/bin/sh
# Gradle wrapper
SCRIPT_DIR=$(dirname "$0")
exec java -jar "$SCRIPT_DIR/gradle/wrapper/gradle-wrapper.jar" "$@"
