#!/bin/bash
export JDK_NEXT=$(./install-jdk.sh --silent --emit-java-home -F 15 -L GPL)
echo JDK_NEXT: $JDK_NEXT