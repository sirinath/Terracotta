#!/bin/bash

cd `dirname $0`

for i in lib/*.jar; do
  jarsigner -keystore resources/tc.keys -storepass terracotta $i license-generator -keypass terracotta
done
