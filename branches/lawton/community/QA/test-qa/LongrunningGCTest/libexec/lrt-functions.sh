#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

sandbox=`pwd`
repo="${sandbox}/repo"
tc="${sandbox}/terracotta"
lrt_home="${repo}/code/test-qa/LongrunningGCTest"
cfg="${lrt_home}/conf/tc-config.xml"
java="java"
dsojava="${tc}/dso/bin/dso-java.sh"
startserver="${tc}/dso/bin/start-tc-server.sh"
