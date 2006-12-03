#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

#!/bin/bash
#-------------------------------------------------------------------------------
# Executes the http load test
#-------------------------------------------------------------------------------

if [ "$#" != "4" ]; then
        echo "Usage:"
        echo "  $0 <full classname> <duration> <working dir> <jarpath>"
        exit 3
fi

classname=$1
duration=$2
wkdir=$3
jars=$4

echo ""
echo "****************************************************************************************************"
echo ""
echo "EXECUTING TEST"
echo $HOSTNAME
echo pwd: $wkdir
echo "java" -Xms256m -Xmx512m -classpath ${jars}../lib/classes $classname $duration $wkdir
echo ""
echo "****************************************************************************************************"
echo ""

${JAVA_HOME}/bin/java -Xms1024m -Xmx1280m -classpath ${jars}../lib/classes $classname $duration $wkdir
