#!/bin/bash

wget -nv https://download.java.net/java/early_access/loom/5/openjdk-19-loom+5-429_linux-x64_bin.tar.gz
tar -zxvf openjdk-19-loom+5-429_linux-x64_bin.tar.gz
export JDK_NEXT=${PWD}/jdk-19
eval "$JDK_NEXT/bin/java -version"
echo "JDK_NEXT=$JDK_NEXT" >> $GITHUB_ENV

#echo "Using older Gradle"
#mv -f gradle/wrapper/gradle-wrapper_loom.properties gradle/wrapper/gradle-wrapper.properties
#echo "Using Loom gradle.build"
#mv -f build_loom.gradle build.gradle
