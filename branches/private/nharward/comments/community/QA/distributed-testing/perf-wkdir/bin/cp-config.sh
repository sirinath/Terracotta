#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

#!/bin/bash
#-------------------------------------------------------------------------------
# This script will copy the tc-config.xml to the L2
#-------------------------------------------------------------------------------

ssh $L2_SERVER "cd $wkdir; rm -rf instance; mkdir instance"
scp ../tc-config.xml ${L2_SERVER}:~/${wkdir}/instance/
	
