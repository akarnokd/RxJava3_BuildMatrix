#!/bin/bash

wget -nv https://download.java.net/java/GA/jdk20/bdc68b4b9cbc4ebcb30745c85038d91d/36/GPL/openjdk-20_linux-x64_bin.tar.gz
tar -zxvf openjdk-20_linux-x64_bin.tar.gz
export JDK_NEXT=${PWD}/jdk-20
eval "$JDK_NEXT/bin/java -version"
echo "JDK_NEXT=$JDK_NEXT" >> $GITHUB_ENV

#echo "Using older Gradle"
#mv -f gradle/wrapper/gradle-wrapper_loom.properties gradle/wrapper/gradle-wrapper.properties
#echo "Using Loom gradle.build"
#mv -f build_loom.gradle build.gradle
