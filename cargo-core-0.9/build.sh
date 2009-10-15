#!/bin/bash
mvn clean install -Dmaven.test.skip=true
jar=~/source/trunk/community/code/base/dependencies/lib/cargo-core-uberjar-0.9.jar
rm -rf $jar
cp uberjar/target/cargo-core-uberjar-0.9.jar $jar
