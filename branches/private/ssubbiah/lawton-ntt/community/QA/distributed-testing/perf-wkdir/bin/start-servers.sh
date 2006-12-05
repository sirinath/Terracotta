#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

#!/bin/bash
#-------------------------------------------------------------------------------
# This script will start the server on each host:
#-------------------------------------------------------------------------------

if [ "$#" -lt "1" ]; then
        echo "Usage:"
        echo "  $0 <tomcat or weblogic> [_clustered]"
        exit 3
fi

container=$1
PIDS=

if [ "$container" = "weblogic" ]; then
	if [ -n "${2}" ]; then
		ssh $L2_SERVER "export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:${WEBLOGIC_HOME}/server/lib/linux/`uname -m`; cd ~/${wkdir}/wls-admin; $WEBLOGIC_JAVA_HOME/bin/java -client -Xms256m -Xmx512m -classpath ${WEBLOGIC_HOME}/server/lib/weblogic.jar -Dbea.home="'${PWD}'" -Dweblogic.management.username=tc -Dweblogic.management.password=tc -Dweblogic.Name=tc-wls-admin-server -Dweblogic.ProductionModeEnabled=true -Djava.security.policy=${WEBLOGIC_HOME}/server/lib/weblogic.policy weblogic.Server > logs/weblogic-admin.std.log 2>&1 &" &
		echo "STARTING WLS ADMIN SERVER: sleep for 30 seconds"
		sleep 30
	fi
fi

for i in $L1_CLIENTS
do
	if [ "$container" = "tomcat" ]; then
		ssh $i "cd ~/${wkdir}/instance; ./start-server.sh ${container} ${i}-server ${2}" &
	fi
	if [ "$container" = "weblogic" ]; then
		if [ -n "${2}" ]; then
			ssh $i "cd ~/${wkdir}/instance; ./start-server.sh ${container} ${i}-managed ${2} http://$L2_SERVER:7001" &
		else
			ssh $i "cd ~/${wkdir}/instance; ./start-server.sh ${container} ${i}-managed" &
		fi
	fi
	
	PIDS="${PIDS} $!"
done

for pid in $PIDS; do
    wait "${pid}"
done 
