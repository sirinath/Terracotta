#!/bin/bash
#
#  All content copyright (c) 2003-2008 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

test -z $JAVA_HOME_16 && "Need to define JAVA_HOME_16" && exit 1
export JAVA_HOME=$JAVA_HOME_16
test ! -d $JAVA_HOME && echo "${JAVA_HOME} does not exist." && exit 1

test -z $TC_INSTALL_DIR && export TC_INSTALL_DIR=`pwd`/../.. 
test \! -f ${TC_INSTALL_DIR}/lib/tc.jar && echo "Need to define TC_INSTALL_DIR" && exit 1

echo "JAVA_HOME:      ${JAVA_HOME}"
echo "TC_INSTALL_DIR: ${TC_INSTALL_DIR}"
echo "--"