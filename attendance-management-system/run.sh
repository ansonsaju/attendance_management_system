#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")"
rm -rf out
mkdir -p out
javac -d out $(find src -name "*.java")
java -cp "out:lib/*" com.attendance.Main .
