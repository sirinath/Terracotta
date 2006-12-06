#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

#!/bin/bash
#-------------------------------------------------------------------------------
# This script will invoke a thread-dump on each machine in the cluster
#-------------------------------------------------------------------------------

cd $(dirname $0)
. assign-hosts.sh
. env.sh

if [ "$#" != "2" ]; then
        echo "Usage:"
        echo "  $0 <delay in MICROSECONDS between dumps> <number of thread-dumps>"
        exit 3
fi

delay=$1
dumps=$2
dumpcount=0

cmd="wkdir=${wkdir}; "'count=0; while [ $count -lt __DUMPS__ ]; do kill -3 `cat ~/${wkdir}/instance/PID`; usleep __DELAY__; count=`expr $count + 1`; done'
cmd=${cmd/__DUMPS__/$dumps}
cmd=${cmd/__DELAY__/$delay}

for i in $L1_CLIENTS $L2_SERVER
do
  ssh $i "$cmd" &
done

wait


