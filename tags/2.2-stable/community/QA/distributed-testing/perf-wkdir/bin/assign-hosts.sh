#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

#!/bin/bash
#-------------------------------------------------------------------------------
# Reads a properties file of the format:
# 
# MASTER <host>
# L2 <host>
# L1 <host>:<...>
# 2ndLOAD <host>:<...>
#-------------------------------------------------------------------------------

count=0
for i in `awk '{print $2}' < ../hosts`
do
	eval "hosts$count=$i"
	count=`expr $count + 1`
done

export MASTER_NODE=${hosts0}
export L2_SERVER=${hosts1}
export L1_CLIENTS=`expr $hosts2 | sed s/:/' '/g`
if [ -n "${hosts3}" ]; then
	export OTHER_LOAD_CLIENTS=`expr $hosts3 | sed s/:/' '/g`
fi

export ALL_HOSTS_MINUS_LOAD="$L1_CLIENTS $L2_SERVER"
export ALL_HOSTS="$L1_CLIENTS $MASTER_NODE $L2_SERVER $OTHER_LOAD_CLIENTS"

#echo MASTER_NODE=$MASTER_NODE
#echo L2_SERVER=$L2_SERVER
#echo L1_CLIENTS=$L1_CLIENTS
#echo OTHER_LOAD_CLIENTS=$OTHER_LOAD_CLIENTS
