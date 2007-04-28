#!/bin/sh

# IBM Confidential OCO Source Material
# 5724-I63, 5724-H88, 5655-N01, 5733-W61 (C) COPYRIGHT International Business Machines Corp. 1997, 2006
# The source code for this program is not published or otherwise divested
# of its trade secrets, irrespective of what has been deposited with the
# U.S. Copyright Office.

binDir=`dirname $0`
. $binDir/setupCmdLine.sh

#CONSOLE_ENCODING controls the output encoding used for stdout/stderr
#    console - encoding is correct for a console window
#    file    - encoding is the default file encoding for the system
#    <other> - the specified encoding is used.  e.g. Cp1252, Cp850, SJIS

PLATFORM=`/bin/uname`
case $PLATFORM in
  AIX)
    EXTSHM=ON
    LIBPATH="$WAS_LIBPATH":$LIBPATH
    CONSOLE_ENCODING=-Dws.output.encoding=console
    export LIBPATH EXTSHM CONSOLE_ENCODING;;
  Linux)
    LD_LIBRARY_PATH="$WAS_LIBPATH":$LD_LIBRARY_PATH
    CONSOLE_ENCODING=-Dws.output.encoding=console
    export LD_LIBRARY_PATH CONSOLE_ENCODING;;
  SunOS)
    LD_LIBRARY_PATH="$WAS_LIBPATH":$LD_LIBRARY_PATH
    CONSOLE_ENCODING=-Dws.output.encoding=console
    export LD_LIBRARY_PATH CONSOLE_ENCODING;;
  HP-UX)
    SHLIB_PATH="$WAS_LIBPATH":$SHLIB_PATH
    CONSOLE_ENCODING=-Dws.output.encoding=console
    export SHLIB_PATH CONSOLE_ENCODING;;
  OS/390|z/OS)
    ZOPTIONS="-Xnoargsconversion -Dfile.encoding=ISO-8859-1 $JVM_EXTRA_CMD_ARGS"
    ZINPUT_HANDLER="-inputhandler com.ibm.ws.ant.utils.WebSphereInputHandler";;
esac

while [ ${#} -gt 0 ]
do
        case "${1}" in
                *\"*\'*|*\'*\"*) echo "$1" | sed "s:\":\\\\\\\&:g; s:.*:\"&\":" | read argv;;
                *\"*) argv="'$1'";;
                *) argv="\"$1\"";;
        esac
        shift
        ARGS="${ARGS} ${argv}"
done

ARGS="${ARGS} -Dbasedir=$USER_INSTALL_ROOT/samples -Dwas_home=$WAS_HOME"

WAS_EXT_DIRS="$WAS_HOME"/plugins:$WAS_EXT_DIRS

WAS_ANT_EXTRA_CLASSPATH="$WAS_HOME/lib/bootstrap.jar:$WAS_HOME/lib/j2ee.jar:$WAS_HOME/optionalLibraries/jython.jar:$WAS_HOME/optionalLibraries/jython/jython.jar"
WAS_ANT_CLASSPATH=$WAS_ANT_CLASSPATH:$WAS_HOME/plugins:$WAS_HOME/optionalLibraries/jython.jar:$WAS_HOME/lib/j2ee.jar

WAS_ANT_CLASSPATH=$WAS_ANT_CLASSPATH:"$USER_INSTALL_ROOT/samples/bld/WebServicesSamples/addrEJBBottomUp"
WAS_ANT_CLASSPATH=$WAS_ANT_CLASSPATH:"$USER_INSTALL_ROOT/samples/bld/WebServicesSamples/addrBeanBottomUp"

# Additional classpath elements needed by WebServicesSamples for BottomUp beans.
# Ant requires classpath elements to exist before beginning...
mkdir -p "$USER_INSTALL_ROOT/samples/bld/WebServicesSamples/addrEJBBottomUp"
mkdir -p "$USER_INSTALL_ROOT/samples/bld/WebServicesSamples/addrBeanBottomUp"

eval "$JAVA_HOME/bin/java" "$OSGI_INSTALL" "$OSGI_CFG" $WAS_LOGGING "$CONSOLE_ENCODING" "$CLIENTSAS" "$CLIENTSSL" -Dwas.ant.extra.classpath="$WAS_ANT_EXTRA_CLASSPATH" -DWAS_USER_SCRIPT="$WAS_USER_SCRIPT" "$USER_INSTALL_PROP" -Dwas.install.root="$WAS_HOME" -Dwas.root="$WAS_HOME" -Dprereq.classpath="$WAS_HOME/plugins/com.ibm.ws.runtime_6.1.0.jar" -Dws.ext.dirs="$WAS_EXT_DIRS" ${ZOPTIONS:=} -classpath "$WAS_CLASSPATH:$WAS_ANT_CLASSPATH" com.ibm.wsspi.bootstrap.WSPreLauncher -nosplash  -application com.ibm.ws.bootstrap.WSLauncher com.ibm.ws.runtime.LaunchWSAnt ${ZINPUT_HANDLER:=} ${ARGS}
