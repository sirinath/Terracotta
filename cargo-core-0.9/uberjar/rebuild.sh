#!/bin/bash

workdir=target
file=cargo-core-uberjar-0.9

cd $workdir
jar xf $file.jar

for i in *.jar; do
  if [ -d $i ]; then
    cp -r $i/* .
  fi
done

jar cf $file-tc-20080103.jar META-INF org
