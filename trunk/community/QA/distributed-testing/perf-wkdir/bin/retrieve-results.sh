#!/bin/bash
#-------------------------------------------------------------------------------
# This script will retrieve the server logs and .obj files from each host:
#-------------------------------------------------------------------------------
rootDir=$(cd $(dirname $0)/..; pwd)
if [ "$#" != "1" ]; then
        echo "Usage:"
        echo "  $0 <tomcat or weblogic>"
        exit 3
fi

container=$1

mkdir ../logs

for i in $ALL_HOSTS_MINUS_LOAD
do
	scp $i:~/${wkdir}/instance/results/* ${rootDir}/results
	mkdir -p ${rootDir}/logs/${i}
	scp -r $i:~/${wkdir}/instance/logs/* ${rootDir}/logs/${i}
done
