### Status
[![Circle CI](https://circleci.com/gh/xjdr/xio.svg?style=svg)](https://circleci.com/gh/xjdr/xio)

[![Coverage Status](https://coveralls.io/repos/xjdr/xio/badge.svg?branch=master&service=github)](https://coveralls.io/github/xjdr/xio?branch=master)

[![Snapshot Repository](https://img.shields.io/badge/xio-0.13.0--SNAPSHOT-green.svg)](https://packagecloud.io/pdex/xio) [Snapshot install instructions](https://packagecloud.io/pdex/xio/install#maven)

xio
===

High performance Multithreaded non-blocking Async I/O for Java 8

`Simplicity Leads to Purity - Jiro`

## Xio is a network library used to build high performance, scalable network applications

Full readme and docs coming soon, to see sample uses, take a look at the tests.

working with the codebase
=========================

### lombok

This project uses the following lombok features:

 * https://projectlombok.org/features/GetterSetter.html
 * https://projectlombok.org/features/ToString.html
 * https://projectlombok.org/features/Data.html
 * https://projectlombok.org/features/Value.html
 * https://projectlombok.org/features/Builder.html
 * https://projectlombok.org/features/Log.html

### github flow

This project is using github flow: https://guides.github.com/introduction/flow/

### Source Code Style

`xio` source code conforms to the standards set forth in the [Google
Java Style Guide](https://google.github.io/styleguide/javaguide.html). The
following maven plugins maintain the source code standards:

 * [maven-git-code-format](https://github.com/Cosium/maven-git-code-format) is a
   pre-commit git hook that formats all of the java source code files about to
   be committed.

 * [fmt-maven-plugin](https://github.com/coveo/fmt-maven-plugin) is run during
   `mvn verify` to ensure that source files are formatted correctly.

 * [maven-checkstyle-plugin](https://maven.apache.org/plugins/maven-checkstyle-plugin/index.html)
   is run during `mvn verify` to ensure that style conventions are being adhered to.

### Testing new jdk versions

#### build the docker image

docker build -t xio-jdk9 .

#### test the build

docker run  --mount type=bind,src=`pwd`,dst=/home -t xio-jdk9 ./mvnw verify

#### interactive prompt

docker run  --mount type=bind,src=`pwd`,dst=/home -it xio-jdk9
