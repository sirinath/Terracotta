#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

#!/bin/bash
#-------------------------------------------------------------------------------
# This script will build a tst dist and move it to the perf-wk dir to be
# uploaded to <master-node> by the calling script
#-------------------------------------------------------------------------------

if [ "$#" != "2" ]; then
        echo "Usage:"
        echo "  $0 <~/code/base/> tst|jars"
        exit 3
fi

homedir=$PWD
basedir=$1
which=$2

cd $basedir

if [ $2 == "tst" ]; then
  ./tcbuild dist dso
fi

if [ $2 == "jars" ]; then
  ./tcbuild dist_jars dso
fi

cd build/dist/

if [ $2 == "tst" ]; then
  cp ${homedir}/../license.lic terracotta*/
  zip -rq ${homedir}/../terracotta-tst.zip terracotta*/
fi

if [ $2 == "jars" ]; then
  cd tc-jars
  cp tc.jar ${homedir}/../tc.jar
  cd session
  cp tc-session.jar ${homedir}/../tc-session.jar
fi
