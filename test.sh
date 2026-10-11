#!/usr/bin/env sh
set -e
rm -rf out-test && mkdir -p out-test
javac --release 17 -encoding UTF-8 -d out-test -sourcepath "src/main/java:src/test/java" src/test/java/rush/TestRunner.java
java -ea -cp out-test rush.TestRunner
