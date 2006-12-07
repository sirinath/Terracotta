#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

#!/bin/bash
#-------------------------------------------------------------------------------
# This script is used to start the appservers
#-------------------------------------------------------------------------------

if [ "$#" -lt "2" ]; then
        echo "Usage:"
        echo "  $0 <tomcat or weblogic> <servername> [_clustered & ?(weblogic)[admin url]]"
        exit 3
fi

container=$1
servername=$2
nativecluster=$3
ADMIN_URL=$4

. assign-hosts.sh
. env.sh

rm -rf results
mkdir results

rm -rf logs
mkdir logs

SHORT_HOST=`hostname -s`

echo "STARTING SERVER AT: ${HOSTNAME}"

if [ "$container" = "tomcat" ]; then
	export TC_JAVA_HOME=$TOMCAT_JAVA_HOME
fi
if [ "$container" = "weblogic" ]; then
	export TC_JAVA_HOME=$WEBLOGIC_JAVA_HOME
fi

# Enable DSO for Sessions
if [ -z "${nativecluster}" ]; then
	. ${TC_HOME}/dso/libexec/tc-functions.sh
	tc_install_dir "${TC_HOME}"
	tc_set_dso_boot_jar
	
	JAVA_OPTS="${JAVA_OPTS} -Xbootclasspath/p:${DSO_BOOT_JAR}"
	JAVA_OPTS="${JAVA_OPTS} -Dtc.install-root=${TC_HOME}"
	JAVA_OPTS="${JAVA_OPTS} -Dtc.config=http://${L2_HOST}:9510/config"
fi

JAVA_OPTS="${JAVA_OPTS} -D__TEST_HOSTNAME__=${SHORT_HOST}" # SimpleHttpFieldReplicationTest needs this defined
export JAVA_OPTS

if [ "$container" = "tomcat" ]; then
	export CATALINA_OPTS="${TOMCAT_JAVA_OPTS}"
  ${CATALINA_HOME}/bin/catalina.sh run > logs/tomcat.std.log 2>&1 &
fi

if [ "$container" = "weblogic" ]; then
	export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:${WEBLOGIC_HOME}/server/lib/linux/`uname -m`
	if [ -n "${nativecluster}" ]; then
		$WEBLOGIC_JAVA_HOME/bin/java -client -Xms256m -Xmx512m ${JAVA_OPTS} -classpath ${WEBLOGIC_HOME}/server/lib/weblogic.jar -Dbea.home=${PWD} -Dweblogic.ListenPort=8080 -Dweblogic.Name=${servername} -Dweblogic.management.server=${ADMIN_URL} -Dweblogic.management.username=tc -Dweblogic.management.password=tc -Dweblogic.ProductionModeEnabled=true -Djava.security.policy=${WEBLOGIC_HOME}/server/lib/weblogic.policy weblogic.Server > logs/weblogic.std.log 2>&1 &
	else
		$WEBLOGIC_JAVA_HOME/bin/java -client -Xms256m -Xmx512m ${JAVA_OPTS} -classpath ${WEBLOGIC_HOME}/server/lib/weblogic.jar -Dbea.home=${PWD} -Dweblogic.ListenPort=8080 -Dweblogic.Name= -Dweblogic.management.username=tc -Dweblogic.management.password=tc -Dweblogic.ProductionModeEnabled=true -Djava.security.policy=${WEBLOGIC_HOME}/server/lib/weblogic.policy weblogic.Server > logs/weblogic.std.log 2>&1 &
	fi
fi

echo $! > PID
