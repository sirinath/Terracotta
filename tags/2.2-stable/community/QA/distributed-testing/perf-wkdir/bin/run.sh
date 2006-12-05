#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

#!/bin/bash
#-------------------------------------------------------------------------------
# This script will prep and run an http load test
#-------------------------------------------------------------------------------

if [ "$#" -lt "3" ]; then
        echo "Usage:"
        echo "  $0 <container name> <full classname> <duration> [validate] [nativecluster]"
        exit 3
fi

container=$1
classname=$2
duration=$3

if [ "$4" = "validate" ]; then
	validate=$4		
	if [ "$5" = "nativecluster" ]; then
		export nativecluster=_clustered
	fi
else
	if [ "$4" = "nativecluster" ]; then
		export nativecluster=_clustered
	fi
fi

war=perftest.war

cd $(dirname $0)
. assign-hosts.sh
. env.sh

./killalljava.sh

if [ -z "$nativecluster" ]; then
	./update-tst.sh $container
fi

# clear out any old results
rm -rf ../results
mkdir ../results

for i in $OTHER_LOAD_CLIENTS
do
	ssh $i "rm -rf ${wkdir}/perf-wkdir"
	scp -rq ../../perf-wkdir $i:~/${wkdir}
done

./load-env.sh ${container} ${nativecluster}
./deploy-war.sh ${war} ${container}

if [ -z "$nativecluster" ]; then
	./cp-config.sh
	./start-l2-server.sh
fi

./start-servers.sh ${container} ${nativecluster}

if [ $container = "weblogic" ]; then
	echo "sleep for 100 seconds"
	sleep 100
else
	echo "sleep for 25 seconds"
	sleep 25
fi

./collect-stats.sh start

cd ../
pdir=$PWD
cd bin

for i in ../lib/*.jar ; do jars=$jars"$i:"; done

PIDS=

#for i in $OTHER_LOAD_CLIENTS
#do
#  ssh $i "export JAVA_HOME=${JAVA_HOME}; cd ${wkdir}/perf-wkdir/bin; ./exec-load.sh ${classname} ${duration} ${pdir} ${jars}" &
#	PIDS="${PIDS} $!"
#done

./exec-load.sh ${classname} ${duration} ${pdir} ${jars} &
PIDS="${PIDS} $!"
for pid in $PIDS; do
    wait "${pid}"
done 

./collect-stats.sh stop
./stop-servers.sh
sleep 6
./retrieve-results.sh ${container}

#count=2
#for i in $OTHER_LOAD_CLIENTS
#do
	#scp $i:${pdir}/results/response-statistics.obj ../results/response-statistics_${count}.obj
	#count=`expr $count + 1`
#done

if [ -n "$validate" ]; then
	echo ""
	echo "****************************************************************************************************"
	echo "VALIDATING GRAPH DATA..."
	echo ""

	java -Xms512m -Xmx1024m -classpath ${jars}../lib/classes $classname validate $pdir
fi

echo ""
echo "****************************************************************************************************"
echo ""

java -Xms1024m -Xmx1280m -classpath ${jars}../lib/classes $classname $duration $pdir report > ../report.txt
#java -classpath ${jars}../lib/classes com.tctest.performance.results.ResultsGraph ${wkdir}/results.data save

cat ../report.txt
echo ""
echo ""
echo "****************************************************************************************************"
echo ""
