#!/bin/bash
export JDK14=$(./install-jdk.sh --silent --emit-java-home -F 14 -L GPL)
echo JDK14: $JDK14