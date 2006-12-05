#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

#!/bin/bash
#-------------------------------------------------------------------------------
# This script is meant to be run on the developers machine to transfer files
# from Eclipse to the current sandbox directory
#-------------------------------------------------------------------------------

if [ "$#" != "1" ]; then
        echo "Usage:"
        echo "  $0 <~/code/base/>"
        exit 3
fi

BASE_DIR=$1
LIB=../lib

rm -rf $LIB/*
mkdir -p $LIB/classes
cp $BASE_DIR/dso-performance-tests/lib.tests.base/*.jar $LIB/
cp $BASE_DIR/common/lib/commons-logging.jar $LIB/

if [ -d $BASE_DIR/dso-performance-tests/build.eclipse ]; then
  cp -r $BASE_DIR/dso-performance-tests/build.eclipse/tests.base.classes/com $LIB/classes
else 
  cp -r $BASE_DIR/build/dso-performance-tests/tests.base.classes/com $LIB/classes
fi


