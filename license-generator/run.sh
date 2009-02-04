#!/bin/bash

cd `dirname $0`

if [ `uname | grep CYGWIN` ]; then
  cygwin=true
fi

for i in lib/*.jar; do
  classpath=$i:$classpath
done

if $cygwin; then
  classpath=`cygpath -w -p $classpath`
fi

java -cp "$classpath" com.tc.license.generator.gui.MainFrame
