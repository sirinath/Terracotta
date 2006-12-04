#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

#!/bin/bash
#-------------------------------------------------------------------------------
# This script will prep the container instance directories of each host:
#-------------------------------------------------------------------------------

if [ "$#" -lt "1" ]; then
        echo "Usage:"
        echo "  $0 <tomcat or weblogic> [_clustered]"
        exit 3
fi
if [ "$1" == "tomcat" ]; then
	container=jakarta-tomcat-5.0.28
fi
if [ "$1" == "weblogic" ]; then
	container=weblogic-8.1.SP6
fi

cd $(dirname $0)
. assign-hosts.sh
PIDS=

if [ "$1" == "weblogic" ]; then
	if [ -n "${2}" ]; then
		curdir=$PWD
		cd ../container-setup/wls
		${JAVA_HOME}/bin/java -classpath ${curdir}/../lib/classes com.tctest.performance.util.CreateWLSConfig ${curdir}/../hosts
		cd $curdir
	fi
fi

for i in $L1_CLIENTS
do
	ssh $i "cd $wkdir; rm -rf instance; mkdir instance; chmod u+w *"
	scp start-server.sh ${i}:~/${wkdir}/instance/
	scp env.sh ${i}:~/${wkdir}/instance/
	scp assign-hosts.sh ${i}:~/${wkdir}/instance/
	scp ../hosts ${i}:~/${wkdir}/
	
	if [ "$1" == "weblogic" ]; then
		scp -r ../container-setup/wls/template/* $i:~/${wkdir}/instance/
		scp ../container-setup/wls/config${2}.xml $i:~/${wkdir}/instance/config.xml
	fi
	if [ "$1" == "tomcat" ]; then
		ssh $i "cd $wkdir; unzip -q container-installs/${container}.zip -d instance/; chmod +x instance/${container}/bin/*.sh" &	
	fi
	
	PIDS="${PIDS} $!"
done	

if [ "$1" == "weblogic" ]; then
	if [ -n "$2" ]; then
		ssh $L2_SERVER "cd $wkdir; rm -rf wls-admin; mkdir wls-admin; chmod u+w *"
		scp -r ../container-setup/wls/template/* $L2_SERVER:~/$wkdir/wls-admin/
		ssh $L2_SERVER "cd $wkdir; mkdir wls-admin/rmfilestore; mkdir wls-admin/logs" &
		scp ../container-setup/wls/config${2}.xml $L2_SERVER:~/$wkdir/wls-admin/config.xml	
	fi
fi

for pid in $PIDS; do
    wait "${pid}"
done 

for i in $L1_CLIENTS
do
	if [ "$1" == "tomcat" ]; then
		if [ -n "${2}" ]; then
			scp ../container-setup/tomcat/server.xml $i:~/${wkdir}/instance/${container}/conf/
		fi
	fi
done
		
