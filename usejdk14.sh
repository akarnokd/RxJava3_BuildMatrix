#!/bin/bash
export JDK_NEXT=$(./install-jdk.sh --silent --emit-java-home -F 14 -L GPL)
echo JDK_NEXT: $JDK_NEXT
eval "$JDK_NEXT/bin/java -version"