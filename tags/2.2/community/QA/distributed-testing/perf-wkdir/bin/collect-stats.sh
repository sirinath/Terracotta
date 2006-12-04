#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

#!/bin/bash
#-------------------------------------------------------------------------------
# This script will start and stop vmstat for each machine in the cluster:
#-------------------------------------------------------------------------------

if [ "$#" != "1" ]; then
        echo "Usage:"
        echo "  $0 <start | stop>"
        exit 3
fi

interval=2
action=$1
cd $(dirname $0)
. assign-hosts.sh

if [ "$action" = "start" ]; then
	for i in $ALL_HOSTS_MINUS_LOAD
	do
		ssh $i "cd $wkdir; mkdir -p instance/logs; cd instance/logs; echo $interval second interval starting at `date` > vmstat.log; nohup vmstat $interval >> vmstat.log 2>&1 &" &
	done
fi

if [ "$action" = "stop" ]; then
	for i in $ALL_HOSTS_MINUS_LOAD
	do
		ssh $i "killall -9 vmstat"
	done
fi
		
