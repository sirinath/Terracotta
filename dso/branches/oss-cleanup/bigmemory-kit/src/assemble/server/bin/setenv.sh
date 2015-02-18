#!/bin/sh

#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
#

if [ -d "$TC_INSTALL_DIR" ] ; then
  if [ -r "$TC_INSTALL_DIR"/../install/bin/setenv.sh ] ; then
    . "$TC_INSTALL_DIR"/../install/bin/setenv.sh
  fi
fi
