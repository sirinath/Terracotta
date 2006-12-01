#!/bin/bash
#-------------------------------------------------------------------------------
# This script will copy the tc-config.xml to the L2
#-------------------------------------------------------------------------------

ssh $L2_SERVER "cd $wkdir; rm -rf instance; mkdir instance"
scp ../tc-config.xml ${L2_SERVER}:~/${wkdir}/instance/
	
