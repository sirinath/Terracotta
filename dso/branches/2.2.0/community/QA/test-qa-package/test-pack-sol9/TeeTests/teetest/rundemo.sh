#!/bin/sh

##
# COPYRIGHT NOTICE
#
# All content copyright Terracotta, Inc. 2005-2006.  All rights reserved.  Any
# copying, re-distribution, or sale is expressly prohibited.  Violators
# will be prosecuted to the maximum extent permissible under applicable law.
#


. ${TC_INSTALL_DIR:-`dirname $0`/../..}/libexec/tc-functions.sh

tc_install_dir `dirname $0`/../.. true
tc_classpath "`dirname $0`/classes" false
tc_java_opts ""
tc_config "`dirname $0`/tc-config.xml"

run_dso_java -classpath "${TC_CLASSPATH}" "${D_TC_CONFIG}" ${TC_ALL_JAVA_OPTS} demo.consolechat.Chatter "kalai"
