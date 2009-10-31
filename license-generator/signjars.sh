#!/bin/bash

cd `dirname $0`

for i in lib/*.jar; do
  jarsigner -keystore resources/keystore.jks -storepass terracotta $i terracotta -keypass terracotta
done
