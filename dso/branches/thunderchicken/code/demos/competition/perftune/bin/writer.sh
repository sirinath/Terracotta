#!/bin/bash

BASEDIR=`dirname $0`/..
cd $BASEDIR
. $BASEDIR/bin/env.sh

test $? -ne 0 && exit 1

for i in $BASEDIR/lib/*.jar ; do
   if test ! -z $CLASSPATH; then
      CLASSPATH=${CLASSPATH}:$i
   else
      CLASSPATH=$i
   fi
done

export TC_CONFIG_PATH=$BASEDIR/tc-config.xml
. $TC_INSTALL_DIR/bin/dso-env.sh -q

$JAVA_HOME/bin/java -Xmx1536m -Xms1536m  $TC_JAVA_OPTS -Dtc.node-name=stress_ingestor -cp $CLASSPATH  com.tctest.perf.dashboard.stats.app.test.Ingestor
