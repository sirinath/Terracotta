#!/bin/sh

. ../../../bin/setupCmdLine.sh

# ANT tools are specific to the zOS and OS400 platforms.
# Detect the platform and set the suffix.
# Use suffix to identify the bundle fragment where the ANT tools are located.
# Add the bundle fragment jar to the ANTCLASSPATH.
PLATFORM=`/bin/uname`
export PLATFORM
case $PLATFORM in
  OS400)
    XPLAT="os400" ;;
  OS/390)
    XPLAT="ws390" ;;
  *)
    XPLAT="dist" ;;
esac

ANTCLASSPATH="$JAVA_HOME/lib/tools.jar:$WAS_HOME/plugins/com.ibm.ws.runtime_6.1.0.jar:$WAS_HOME/plugins/com.ibm.ws.runtime."$XPLAT"_6.1.0.jar:$CLASSPATH"

# Additional jars needed by PlantsByWebSphere Sample
ANTCLASSPATH="$ANTCLASSPATH:$WAS_HOME/lib/j2ee.jar:$WAS_HOME/plugins/com.ibm.ws.emf_2.1.0.jar:$WAS_HOME/plugins/com.ibm.ws.wccm_6.1.0.jar:$WAS_HOME/lib/bootstrap.jar:$WAS_HOME/plugins/com.ibm.ws.bootstrap_6.1.0.jar:$WAS_HOME/plugins/org.eclipse.core.runtime_3.1.2.jar"
                                                 
# Additional classpath elements needed by PlantsByWebSphere Sample.
ANTCLASSPATH=$ANTCLASSPATH:"$USER_INSTALL_ROOT/samples/bld/PlantsByWebSphere"

# Ant requires classpath elements to exist before beginning...
mkdir -p "$USER_INSTALL_ROOT/samples/bld/PlantsByWebSphere"

export ANTCLASSPATH

"$JAVA_HOME/jre/bin/java" -classpath $ANTCLASSPATH org.apache.tools.ant.Main -Dbasedir="$USER_INSTALL_ROOT/samples" -Dwas_home="$WAS_HOME" $@
