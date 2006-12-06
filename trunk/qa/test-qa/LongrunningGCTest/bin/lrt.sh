##!/bin/sh
#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#
TOPDIR=`dirname $0`/..
. ${TOPDIR}/libexec/lrt-functions.sh
host="localhost"
server_host="localhost"

build_root="${repo}/code/base/build"
test_class_path_root="${build_root}/dso-system-tests"
simulator_classes="${build_root}/simulator/tests.base.classes"

classpath="${test_class_path_root}/tests.base.classes:${test_class_path_root}/tests.system.classes:${simulator_classes}:${tc}/common/lib/tc.jar"

verbosegc="-XX:+PrintGCTimeStamps -XX:+PrintGCDetails -verbose:gc"

client_cmd="${dsojava} ${verbosegc} -Dtc.config=http://${server_host}:9515/config -classpath $classpath com.tctest.longrunning.LongrunningGCTestAppCLI participantcount=3 client://${host}${sandbox}?vm.count=3&execution.count=1&jvm.args=-XX:+PrintGCTimeStamps,-XX:+PrintGCDetails,-verbose:gc server://${host}${sandbox}?jvm.args=-XX:+PrintGCTimeStamps,-XX:+PrintGCDetails,-verbose:gc,-server,-Xms256m,-Xmx256m,-Xss128k intensity=1"

server_cmd="${startserver} -f ${cfg}"

export TC_JAVA_OPTS=$verbosegc

echo "LongrunningGCTest..."
echo "sandbox    : ${sandbox}"
echo "tc         : ${tc}"
echo "client host: ${host}"
echo "server host: ${server_host}"
echo "config     : ${cfg}"

echo $server_cmd
$server_cmd > ${sandbox}/server.txt 2>&1 &

echo $client_cmd
$client_cmd > ${sandbox}/client1.txt 2>&1 &

echo $client_cmd
$client_cmd > ${sandbox}/client2.txt 2>&1 &

echo $client_cmd
$client_cmd > ${sandbox}/client3.txt 2>&1 &


