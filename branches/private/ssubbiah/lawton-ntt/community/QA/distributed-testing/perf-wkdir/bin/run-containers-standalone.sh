#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

#!/bin/bash

if [ "$#" -lt "1" ]; then
        echo "Usage:"
        echo "  $0 <container name> [nativecluster]"
        exit 3
fi

container=$1
if [ -n "${2}" ]; then
	nativecluster=_clustered
fi
war=perftest.war

cd $(dirname $0)
. assign-hosts.sh
. env.sh

./killalljava.sh
./load-env.sh ${container} ${nativecluster}
./deploy-war.sh ${war} ${container}
./start-servers.sh ${container} ${nativecluster}

if [ $container = "weblogic" ]; then
	echo "sleep for 100 seconds"
	sleep 100
else
	echo "sleep for 15 seconds"
	sleep 15
fi

echo "SERVERS STARTED"
