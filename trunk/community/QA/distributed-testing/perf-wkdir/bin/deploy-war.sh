#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

#!/bin/bash
#-------------------------------------------------------------------------------
# This script deploys the war (argument) to the server on each host:
#-------------------------------------------------------------------------------

if [ "$#" != "2" ]; then
        echo "Usage:"
        echo "  $0 <war file name> <tomcat or weblogic>"
        exit 3
fi

war=$1
container=$2
cd $(dirname $0)
. assign-hosts.sh

for i in $L1_CLIENTS
do
	if [ "$container" = "tomcat" ]; then
		scp ../${war} $i:${CATALINA_HOME}/webapps
	fi
	if [ "$container" = "weblogic" ]; then
    ssh $i mkdir -p ${wkdir}/instance/applications
		scp ../${war} $i:${wkdir}/instance/applications
	fi
done

if [ "$container" = "weblogic" ]; then
	scp ../${war} $L2_SERVER:${wkdir}/wls-admin/applications
fi
