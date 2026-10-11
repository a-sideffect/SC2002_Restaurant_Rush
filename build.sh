#!/usr/bin/env sh
set -e
rm -rf out && mkdir -p out
javac --release 17 -encoding UTF-8 -d out -sourcepath src/main/java src/main/java/rush/Main.java
echo "Built into out/"
