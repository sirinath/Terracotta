#!/bin/bash

. ./env.sh


if test \! -d "${JAVA_HOME}"; then
   echo "$0: the JAVA_HOME environment variable is not defined
correctly"
   exit 2
fi

#TC_INSTALL_DIR=`dirname "$0"`/..
if test \! -d "${TC_INSTALL_DIR}"; then
   echo "$0: the TC_INSTALL_DIR environment variable is not defined
correctly"
   exit 2
fi

for i in ../lib/*.jar ; do
  if [ "$CLASSPATH" != "" ]; then
       CLASSPATH=${CLASSPATH}:$i
  else
    CLASSPATH=$i
  fi
done
 
export TC_CONFIG_PATH=../tc-config.xml
. $TC_INSTALL_DIR/bin/dso-env.sh -q

$JAVA_HOME/bin/java -Xmx1536m -Xms1536m  $TC_JAVA_OPTS -Dtc.node-name=stress_ingestor -cp $CLASSPATH  com.tctest.perf.dashboard.stats.app.test.Ingestor
