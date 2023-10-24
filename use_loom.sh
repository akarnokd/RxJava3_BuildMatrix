#!/bin/bash

wget -nv https://download.java.net/java/early_access/jdk22/20/GPL/openjdk-22-ea+20_linux-x64_bin.tar.gz
tar -zxvf openjdk-22-ea+20_linux-x64_bin.tar.gz
export JDK_NEXT=${PWD}/jdk-20
eval "$JDK_NEXT/bin/java -version"
echo "JDK_NEXT=$JDK_NEXT" >> $GITHUB_ENV

#echo "Using older Gradle"
#mv -f gradle/wrapper/gradle-wrapper_loom.properties gradle/wrapper/gradle-wrapper.properties
#echo "Using Loom gradle.build"
#mv -f build_loom.gradle build.gradle
