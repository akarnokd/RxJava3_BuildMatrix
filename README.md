# RxJava 3 built with JDK 9 through 16

<a href='https://travis-ci.org/akarnokd/RxJava3_BuildMatrix/builds'><img src='https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master'></a>
[![codecov.io](http://codecov.io/github/akarnokd/RxJava3_BuildMatrix/coverage.svg?branch=master)](https://codecov.io/gh/akarnokd/RxJava3_BuildMatrix/branch/master)

This repository is used for building [RxJava 3](https://github.com/ReactiveX/RxJava) with JDK 9 through 16 in a CI environment. 

It **does not use the JDK 9 `Flow` API** nor does this repository create maven jars.

This repository doesn't produce maven artifacts of the builds as the only purpose is to cross test RxJava **and** the mainstream/upcoming JDKs.

### Build matrix

JDK runtime | Target 8 | Target 9 | Target 10 | Target 11 | Target 12 | Target 13 | Target 14 | Target 15 |
-|----|---|------|------|------|----------|----------|---|
16 |  N/A |  N/A |  N/A | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/1)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/2)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/3)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/4)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/5)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/6)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | 
15 |  N/A |  N/A | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/7)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/8)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/9)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/10)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/11)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/12)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) |  | 
14 |  N/A | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/13)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/14)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/15)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/16)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/17)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/18)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) |  |  | 
13 | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/19)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/20)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/21)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/22)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/23)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/24)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) |  |  |  | 
12 | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/25)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/26)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/27)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/28)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/29)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) |  |  |  |  | 
11 | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/30)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/31)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/32)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/33)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) |  |  |  |  |  | 
10 | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/34)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/35)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/36)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) |  |  |  |  |  |  | 
9 | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/37)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) | [![image](https://travis-matrix-badges.herokuapp.com/repos/akarnokd/RxJava3_BuildMatrix/branches/master/38)](https://travis-ci.org/akarnokd/RxJava3_BuildMatrix.svg?branch=master) |  |  |  |  |  |  |  | 

#### Note on JDK target support

With the newest JDKs, certain older compile targets are no longer supported:
- JDK 12 dropped target 6 support
- JDK 13 dropped target 7 support
- JDK 14 dropped target 8 support
- JDK 15 dropped target 9 support
- JDK 15 dropped target 10 support

Note also that RxJava 3 is now a Java 8 library and thus the Java 6 build targets have been removed.
