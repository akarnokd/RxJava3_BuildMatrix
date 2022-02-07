# RxJava 3 built with JDK 9 through 19

[![codecov.io](http://codecov.io/github/akarnokd/RxJava3_BuildMatrix/coverage.svg?branch=master)](https://codecov.io/gh/akarnokd/RxJava3_BuildMatrix/branch/master)

| JDK | Status |
|----:|--------|
| 19  | <a href='https://github.com/akarnokd/RxJava3_BuildMatrix/actions?query=workflow%3A%22Build_JDK19%22'><img src='https://github.com/akarnokd/RxJava3_BuildMatrix/workflows/Build_JDK19/badge.svg'></a> |
| 18  | <a href='https://github.com/akarnokd/RxJava3_BuildMatrix/actions?query=workflow%3A%22Build_JDK18%22'><img src='https://github.com/akarnokd/RxJava3_BuildMatrix/workflows/Build_JDK18/badge.svg'></a> |
| 17  | <a href='https://github.com/akarnokd/RxJava3_BuildMatrix/actions?query=workflow%3A%22Build_JDK17%22'><img src='https://github.com/akarnokd/RxJava3_BuildMatrix/workflows/Build_JDK17/badge.svg'></a> |
| 16  | <a href='https://github.com/akarnokd/RxJava3_BuildMatrix/actions?query=workflow%3A%22Build_JDK16%22'><img src='https://github.com/akarnokd/RxJava3_BuildMatrix/workflows/Build_JDK16/badge.svg'></a> |
| 15  | <a href='https://github.com/akarnokd/RxJava3_BuildMatrix/actions?query=workflow%3A%22Build_JDK15%22'><img src='https://github.com/akarnokd/RxJava3_BuildMatrix/workflows/Build_JDK15/badge.svg'></a> |
| 14  | <a href='https://github.com/akarnokd/RxJava3_BuildMatrix/actions?query=workflow%3A%22Build_JDK14%22'><img src='https://github.com/akarnokd/RxJava3_BuildMatrix/workflows/Build_JDK14/badge.svg'></a> |
| 13  | <a href='https://github.com/akarnokd/RxJava3_BuildMatrix/actions?query=workflow%3A%22Build_JDK13%22'><img src='https://github.com/akarnokd/RxJava3_BuildMatrix/workflows/Build_JDK13/badge.svg'></a> |
| 12  | <a href='https://github.com/akarnokd/RxJava3_BuildMatrix/actions?query=workflow%3A%22Build_JDK12%22'><img src='https://github.com/akarnokd/RxJava3_BuildMatrix/workflows/Build_JDK12/badge.svg'></a> |
| 11  | <a href='https://github.com/akarnokd/RxJava3_BuildMatrix/actions?query=workflow%3A%22Build_JDK11%22'><img src='https://github.com/akarnokd/RxJava3_BuildMatrix/workflows/Build_JDK11/badge.svg'></a> |
| 10  | <a href='https://github.com/akarnokd/RxJava3_BuildMatrix/actions?query=workflow%3A%22Build_JDK10%22'><img src='https://github.com/akarnokd/RxJava3_BuildMatrix/workflows/Build_JDK10/badge.svg'></a> |
| 9  | <a href='https://github.com/akarnokd/RxJava3_BuildMatrix/actions?query=workflow%3A%22Build_JDK09%22'><img src='https://github.com/akarnokd/RxJava3_BuildMatrix/workflows/Build_JDK09/badge.svg'></a> |
| Loom  | <a href='https://github.com/akarnokd/RxJava3_BuildMatrix/actions?query=workflow%3A%22Build_Loom%22'><img src='https://github.com/akarnokd/RxJava3_BuildMatrix/workflows/Build_Loom/badge.svg'></a> |


(GitHub actions do not have a means to display the individual matrix results in a workflow.)

This repository is used for building [RxJava 3](https://github.com/ReactiveX/RxJava) with JDK 9 through 18 in a CI environment. 

It **does not use the JDK 9 `Flow` API** nor does this repository create maven jars.

This repository doesn't produce maven artifacts of the builds as the only purpose is to cross test RxJava **and** the mainstream/upcoming JDKs.

#### Note on JDK target support

With the newest JDKs, certain older compile targets are no longer supported:
- JDK 12 dropped target 6 support
- JDK 13 dropped target 7 support
- JDK 14 dropped target 8 support
- JDK 15 dropped target 9 support
- JDK 16 dropped target 10 support
- JDK 17 dropped target 11 support
- JDK 18 dropped target 12 support
- JDK 19 dropped target 13 support

Note also that RxJava 3 is now a Java 8 library and thus the Java 6 build targets have been removed.
