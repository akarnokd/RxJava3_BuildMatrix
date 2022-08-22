#!/bin/bash

# doesn't work for some reason, fails with tar exited with code 2
# export JDK_NEXT=$(./install-jdk.sh --silent --emit-java-home -F 16 -L GPL)
# echo JDK_NEXT: $JDK_NEXT

wget -nv https://download.java.net/java/GA/jdk19/877d6127e982470ba2a7faa31cc93d04/36/GPL/openjdk-19_linux-x64_bin.tar.gz
tar -zxvf openjdk-19_linux-x64_bin.tar.gz
export JDK_NEXT=$PWD/jdk-19
eval "$JDK_NEXT/bin/java -version"
echo "JDK_NEXT=$JDK_NEXT" >> $GITHUB_ENV
