#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

#!/bin/bash
echo "STARTING DSO L2 SERVER"
ssh ${L2_SERVER} "export TC_JAVA_OPTS=\"${L2_JAVA_OPTS}\"; export TC_JAVA_HOME=${L2_JAVA_HOME}; cd ${wkdir}/instance; mkdir logs; ${TC_HOME}/dso/bin/start-tc-server.sh -f tc-config.xml > logs/l2.std.log 2>&1 & echo \$! > PID"

