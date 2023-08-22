#!/bin/bash

# doesn't work for some reason, fails with tar exited with code 2
# export JDK_NEXT=$(./install-jdk.sh --silent --emit-java-home -F 16 -L GPL)
# echo JDK_NEXT: $JDK_NEXT

wget -nv https://download.java.net/java/GA/jdk21/fd2272bbf8e04c3dbaee13770090416c/35/GPL/openjdk-21_linux-x64_bin.tar.gz
tar -zxvf openjdk-21_linux-x64_bin.tar.gz
export JDK_NEXT=$PWD/jdk-21
eval "$JDK_NEXT/bin/java -version"
echo "JDK_NEXT=$JDK_NEXT" >> $GITHUB_ENV
