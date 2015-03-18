#!/bin/sh

if [ ! -d "$JAVA_HOME" ]; then
   echo "ERROR: JAVA_HOME must point to Java installation. Please see code-samples/README.txt for more information."
   echo "    $JAVA_HOME"
fi

# You May Need To Change this to your BigMemory installation root
workdir=`dirname $0`
workdir=`cd ${workdir} && pwd`
BIGMEMORY=${workdir}/../..



BIGMEMORY_CP=""



for jarfile in "$BIGMEMORY"/common/lib/*.jar; do
  BIGMEMORY_CP="$BIGMEMORY_CP":$jarfile
done

for jarfile in "$BIGMEMORY"/code-samples/lib/*.jar; do
  BIGMEMORY_CP="$BIGMEMORY_CP":$jarfile
done

for jarfile in "$BIGMEMORY"/apis/ehcache/lib/*.jar; do
  BIGMEMORY_CP="$BIGMEMORY_CP":$jarfile
done


for jarfile in "$BIGMEMORY"/apis/toolkit/lib/*.jar; do
  BIGMEMORY_CP="$BIGMEMORY_CP":$jarfile
done


BIGMEMORY_CP="$BIGMEMORY_CP":

# Convert to Windows path if cygwin detected
# This allows users to use .sh scripts in cygwin
if [ `uname | grep CYGWIN` ]; then
  BIGMEMORY_CP=`cygpath -w -p "$BIGMEMORY_CP"`
fi

