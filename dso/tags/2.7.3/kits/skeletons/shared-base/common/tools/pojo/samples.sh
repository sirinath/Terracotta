#!/bin/sh

#
#  All content copyright (c) 2003-2008 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.
#

if test \! -d "${JAVA_HOME}"; then
  echo "$0: the JAVA_HOME environment variable is not defined correctly"
  exit 2
fi

TC_INSTALL_DIR=`dirname "$0"`/../..

exec "${JAVA_HOME}/bin/java" \
  -Dtc.install-root="${TC_INSTALL_DIR}" \
  -Djava.awt.Window.locationByPlatform=true \
  ${JAVA_OPTS} \
  -cp ${TC_INSTALL_DIR}/lib/tc.jar \
   com.tc.welcome.DSOSamplesFrame "$@"
