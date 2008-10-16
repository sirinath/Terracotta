#!/bin/bash
#
#  All content copyright (c) 2003-2008 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

BASEDIR=`dirname $0`/..
cd $BASEDIR
. $BASEDIR/bin/env.sh

test $? -ne 0 && exit 1

test \! -f $BASEDIR/lib/dashboard.jar && ant clean build 
test \! -f $BASEDIR/lib/dashboard.jar && echo "unable to build project." && exit 1

for i in $BASEDIR/lib/*.jar ; do
   if test \! -z $CLASSPATH; then
      CLASSPATH=${CLASSPATH}:$i
   else
      CLASSPATH=$i
   fi
done

export TC_CONFIG_PATH=$BASEDIR/tc-config.xml
. $TC_INSTALL_DIR/bin/dso-env.sh -q

$JAVA_HOME/bin/java -Xmx1536m -Xms1536m  $TC_JAVA_OPTS -Dtc.node-name=stress_reader -cp $CLASSPATH com.tctest.perf.dashboard.stats.app.test.Reader
