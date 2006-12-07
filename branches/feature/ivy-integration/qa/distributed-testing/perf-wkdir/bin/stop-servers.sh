#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

#!/bin/bash
#-------------------------------------------------------------------------------
# This script will stop the server on each host:
#-------------------------------------------------------------------------------

cd $(dirname $0)
. assign-hosts.sh
PIDS=

for i in $L1_CLIENTS
do
	
echo "STOPPING SERVER AT: ${i}"
	
	ssh $i "killall -9 java" &
	PIDS="${PIDS} $!"
done

for pid in $PIDS; do
    wait "${pid}"
done 

ssh $L2_SERVER "cd $wkdir; export TC_JAVA_HOME=${JAVA_HOME}; terracotta-tst/dso/bin/stop-tc-server.sh"