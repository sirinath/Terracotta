#!/bin/sh
THIS_DIR=`dirname $0`
TC_INSTALL_DIR=`cd $THIS_DIR;pwd`/../..

if [ -r "$TC_INSTALL_DIR"/server/bin/setenv.sh ] ; then
  . "$TC_INSTALL_DIR"/server/bin/setenv.sh
fi
JAVA=$JAVA_HOME/bin/java

workdir=`dirname $0`
workdir=`cd ${workdir} && pwd`
BIGMEMORY=${workdir}/../..

. "${BIGMEMORY}"/code-samples/bin/buildcp.sh

if [ `uname | grep CYGWIN` ]; then
   BIGMEMORY=`cygpath -w -p $BIGMEMORY`
fi

"$JAVA" -Xmx200m  -Dcom.tc.productkey.path="${BIGMEMORY}"/terracotta-license.key  -classpath \'"$BIGMEMORY_CP"\'   com.bigmemory.samples.readwritethrough.ReadWriteThrough
