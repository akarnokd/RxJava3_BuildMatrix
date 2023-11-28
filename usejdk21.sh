#!/bin/bash

# doesn't work for some reason, fails with tar exited with code 2
# export JDK_NEXT=$(./install-jdk.sh --silent --emit-java-home -F 16 -L GPL)
# echo JDK_NEXT: $JDK_NEXT

wget -nv https://download.java.net/java/GA/jdk21.0.1/415e3f918a1f4062a0074a2794853d0d/12/GPL/openjdk-21.0.1_linux-x64_bin.tar.gz
tar -zxvf openjdk-21.0.1_linux-x64_bin.tar.gz
export JDK_NEXT=$PWD/jdk-21
eval "$JDK_NEXT/bin/java -version"
echo "JDK_NEXT=$JDK_NEXT" >> $GITHUB_ENV
