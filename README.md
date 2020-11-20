# RxJava 3 built with JDK 9 through 16

<a href='https://github.com/akarnokd/RxJava3_BuildMatrix/actions?query=workflow%3A%22Java+CI+with+Gradle%22'><img src='https://github.com/akarnokd/RxJava3_BuildMatrix/workflows/Java%20CI%20with%20Gradle/badge.svg'></a>
[![codecov.io](http://codecov.io/github/akarnokd/RxJava3_BuildMatrix/coverage.svg?branch=master)](https://codecov.io/gh/akarnokd/RxJava3_BuildMatrix/branch/master)

This repository is used for building [RxJava 3](https://github.com/ReactiveX/RxJava) with JDK 9 through 16 in a CI environment. 

It **does not use the JDK 9 `Flow` API** nor does this repository create maven jars.

This repository doesn't produce maven artifacts of the builds as the only purpose is to cross test RxJava **and** the mainstream/upcoming JDKs.

### Build matrix

GitHub Actions currently don't support per-matrix element reports.

#### Note on JDK target support

With the newest JDKs, certain older compile targets are no longer supported:
- JDK 12 dropped target 6 support
- JDK 13 dropped target 7 support
- JDK 14 dropped target 8 support
- JDK 15 dropped target 9 support
- JDK 16 dropped target 10 support

Note also that RxJava 3 is now a Java 8 library and thus the Java 6 build targets have been removed.
