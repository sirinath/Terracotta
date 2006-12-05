#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

#!/bin/bash
#-------------------------------------------------------------------------------
# This script will update the tst dist on each host
#-------------------------------------------------------------------------------

cd $(dirname $0)
. assign-hosts.sh

if [ "$#" != "1" ]; then
        echo "Usage:"
        echo "  $0 <container name>"
        exit 3
fi

if [ $1 = "tomcat" ]; then
    JAVA=$TOMCAT_JAVA_HOME
fi


if [ $1 = "weblogic" ]; then
    JAVA=$WEBLOGIC_JAVA_HOME
fi

updatetst=0
test -s ../terracotta-tst.zip && updatetst=1
if [ "$updatetst" == "1" ]; then
	PIDS=

	ssh $L2_SERVER "cd $wkdir; rm -rf terracotta*"
	scp ../terracotta-tst.zip $L2_SERVER:~/${wkdir}
	ssh $L2_SERVER "cd $wkdir; unzip -q terracotta-tst.zip; rm -f terracotta-tst.zip; mv terracotta* terracotta-tst" &
	PIDS="${PIDS} $!"
	
	for i in $L1_CLIENTS
	do
		ssh $i "cd $wkdir; rm -rf terracotta*"
		scp ../terracotta-tst.zip $i:~/${wkdir}
		ssh $i "cd $wkdir; unzip -q terracotta-tst.zip; rm -f terracotta-tst.zip; mv terracotta* terracotta-tst; export TC_JAVA_HOME=${JAVA}; terracotta-tst/dso/bin/make-boot-jar.sh > /dev/null" &
		PIDS="${PIDS} $!"
	done
	for pid in $PIDS; do
    wait "${pid}"
	done 
	
	rm -f ../terracotta-tst.zip
fi


updatejar=0
test -s ../tc.jar && updatejar=1
if [ "$updatejar" == "1" ]; then
	PIDS=

  dest="terracotta-tst/common/lib/tc.jar"
	ssh $L2_SERVER "cd $wkdir; rm -f $dest"
	scp ../tc.jar $L2_SERVER:~/${wkdir}/$dest
	
	for i in $L1_CLIENTS
	do
    sessDest="terracotta-tst/common/lib/session/tc-session.jar"
	  ssh $i "cd $wkdir; rm -f $dest $sessDest"
	  scp ../tc.jar $i:~/${wkdir}/${dest}
    scp ../tc-session.jar $i:~/${wkdir}/${sessDest}
		ssh $i "cd $wkdir; export TC_JAVA_HOME=${JAVA}; terracotta-tst/dso/bin/make-boot-jar.sh > /dev/null" &
		PIDS="${PIDS} $!"
	done
	for pid in $PIDS; do
    wait "${pid}"
	done 
	
	rm -f ../tc.jar ../tc-session.jar
fi
