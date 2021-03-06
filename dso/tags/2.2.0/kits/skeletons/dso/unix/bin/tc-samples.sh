#!/bin/sh

#
#  All content copyright (c) 2003-2006 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

TOPDIR=`dirname $0`/..
. ${TOPDIR}/libexec/tc-functions.sh

tc_install_dir ${TOPDIR}/..
tc_classpath "" true
tc_java_opts "-Dtc.install-root=${TC_INSTALL_DIR} -Djava.awt.Window.locationByPlatform=true"

tc_java -classpath "${TC_CLASSPATH}" ${TC_ALL_JAVA_OPTS} com.tc.welcome.DSOSamplesFrame "$@"
