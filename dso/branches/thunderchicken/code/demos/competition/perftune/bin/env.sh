#!/bin/bash

test -z $TC_INSTALL_DIR && "Need to define TC_INSTALL_DIR" && exit 1
test -z $JAVA_HOME_16   && "Need to define JAVA_HOME_16"   && exit 1

export JAVA_HOME=$JAVA_HOME_16
