#!/bin/bash

# doesn't work for some reason, fails with tar exited with code 2
# export JDK_NEXT=$(./install-jdk.sh --silent --emit-java-home -F 16 -L GPL)
# echo JDK_NEXT: $JDK_NEXT

wget https://download.java.net/java/early_access/jdk17/4/GPL/openjdk-17-ea+4_linux-x64_bin.tar.gz
tar -zxvf openjdk-17-ea+4_linux-x64_bin.tar.gz
export JDK_NEXT=$PWD/jdk-17
eval "$JDK_NEXT/bin/java -version"
echo "JDK_NEXT=$JDK_NEXT" >> $GITHUB_ENV
