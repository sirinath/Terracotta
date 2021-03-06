CWD=`dirname "$0"`
TC_HOME=${CWD}/../../..
JETTY_HOME="${TC_HOME}/vendors/jetty-6.1.15"
JAVA_OPTS="${JAVA_OPTS} -Dcom.sun.management.jmxremote"
JAVA_OPTS="${JAVA_OPTS} -Dcom.sun.management.jmxremote.port=8092"
JAVA_OPTS="${JAVA_OPTS} -Dcom.sun.management.jmxremote.authenticate=false"
JAVA_OPTS="${JAVA_OPTS} -Dcom.sun.management.jmxremote.ssl=false"
JAVA_OPTS="${JAVA_OPTS} -Dtc.node-name=Node2"
JAVA_OPTS="${JAVA_OPTS} -Djetty.home=${JETTY_HOME}"
JAVA_OPTS="${JAVA_OPTS} -Dwebflow.dir=${CWD}"
JAVA_OPTS="${JAVA_OPTS} -Djetty.port=8082"
JAVA_OPTS="${JAVA_OPTS} -Dtc.config=${CWD}/tc-config.xml"
JAVA_OPTS="${JAVA_OPTS} -DSTOP.PORT=8282"
JAVA_OPTS="${JAVA_OPTS} -DSTOP.KEY=secret"
export JAVA_OPTS
exec "${TC_HOME}/bin/dso-java.sh" ${JAVA_OPTS} -jar ${JETTY_HOME}/start.jar ${CWD}/jetty-conf.xml
