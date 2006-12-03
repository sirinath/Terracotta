#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

#!/bin/bash
#-------------------------------------------------------------------------------
# This script is called from the users local machine to scp the current
# sandbox environment to <master-node>
#
# calls: get-env-from-eclipse.sh, gen-organic-graph-war.sh
#-------------------------------------------------------------------------------
umask 002

if [ "$#" -lt "2" ]; then
        echo "Usage:"
        echo "  $0 <remote user> <~/code/base/> [tst|jars]"
        exit 3
fi

remote_user=$1
basedir=$2
cd $(dirname $0)
. assign-hosts.sh
wkdir=working

if [ "$3" != "" ]; then
	./mv-dso.sh $basedir $3
fi

./get-env-from-eclipse.sh ${basedir}
./gen-perf-test-war.sh ${basedir}

ssh $remote_user@$MASTER_NODE "cd $wkdir; rm -rf perf-wkdir"
scp -rq ../../perf-wkdir $remote_user@$MASTER_NODE:~/${wkdir}/

ssh $remote_user@$MASTER_NODE "cd $wkdir; find perf-wkdir -name SCCS -type d | xargs rm -rf"
ssh $remote_user@$MASTER_NODE "cd $wkdir; chmod +x perf-wkdir/bin/*"
if [ -n "`uname -s | grep -i cygwin`" ]; then
  ssh $remote_user@$MASTER_NODE "cd ${wkdir}/perf-wkdir; dos2unix hosts >> /dev/null; cd bin; dos2unix * >> /dev/null 2>&1"
fi


if [ "$3" != "" ]; then
	rm -rf ../terracotta* ../tc.jar ../tc-session.jar
fi
