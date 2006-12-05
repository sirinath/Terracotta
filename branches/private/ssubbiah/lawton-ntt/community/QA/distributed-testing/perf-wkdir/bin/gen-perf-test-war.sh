#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

#!/bin/bash
#-------------------------------------------------------------------------------
# This script is used to build the war used by the series of perf test
# tests
#-------------------------------------------------------------------------------

if [ "$#" != "1" ]; then
        echo "Usage:"
        echo "  $0 <~/code/base/>"
        exit 3
fi

BASE_DIR=$1

cd ../
rm -rf tmp
mkdir tmp
cd tmp
mkdir META-INF
mkdir WEB-INF
cp ../war/perftest/META-INF/context.xml META-INF/
cp ../war/perftest/WEB-INF/web.xml WEB-INF/
cp ../war/perftest/WEB-INF/weblogic.xml WEB-INF/
mkdir WEB-INF/lib
mkdir WEB-INF/classes
cp ../war/perftest/WEB-INF/lib/*.jar WEB-INF/lib/

if [ -d $BASE_DIR/dso-performance-tests/build.eclipse ]; then
  cp -r $BASE_DIR/dso-performance-tests/build.eclipse/tests.base.classes/com WEB-INF/classes/
else 
  cp -r $BASE_DIR/build/dso-performance-tests/tests.base.classes/com WEB-INF/classes/
fi

jar cf ../perftest.war .
cd ../
rm -rf tmp
