#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

JDK14=/usr/java/j2sdk1.4.2_12
JDK15=/usr/java/jdk1.5.0_08

export wkdir=working

export JAVA_HOME=${JDK15}

export PATH=$PATH:${JAVA_HOME}/bin

export TC_HOME=~/${wkdir}/terracotta-tst

export TOMCAT_JAVA_OPTS="-server -Xms1024m -Xmx1024m -verbose:gc -XX:+PrintGCTimeStamps -Dtc.stage.monitor=true -Dtc.stage.monitor.delay=2000 -Dcom.terracotta.session.invalidator.sleep=30"

export TOMCAT_JAVA_HOME=${JDK15}
export CATALINA_HOME=~/${wkdir}/instance/jakarta-tomcat-5.0.28

export WEBLOGIC_JAVA_OPTS="-server -Xms1024m -Xmx1024m -verbose:gc -XX:+PrintGCTimeStamps"
export WEBLOGIC_HOME=~/${wkdir}/container-installs/weblogic-8.1.SP6
export WEBLOGIC_JAVA_HOME=${JDK14}

export L2_HOST=${L2_SERVER}
export L2_JAVA_HOME=/usr/java/jdk1.5.0_08
export L2_JAVA_OPTS="-Xms2048m -Xmx2048m -verbose:gc -XX:+PrintGCTimeStamps -Dtc.stage.monitor=true -Dtc.stage.monitor.delay=2000"
