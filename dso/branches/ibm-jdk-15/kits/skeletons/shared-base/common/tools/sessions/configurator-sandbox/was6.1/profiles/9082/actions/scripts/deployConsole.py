#------------------------------------------------------------------------------
# deployConsole.py - profile creation script for deploying isclite
#------------------------------------------------------------------------------
# Required Arguments:
#    cell   - name of the cell on which to install isclite
#    node   - name of the node on which to install isclite
#    server - name of the server on which to install isclite
#
# Example:
# wsadmin.sh -f deployConsole.py -cell myNodeCell -node myNode -server server1
#
# Notes:
# This script is only inteded to be executed during profile creation
#------------------------------------------------------------------------------
# History:
# 
#    2006/01/26 - wolfpa@us.ibm.com
#    Initial version of deployConsole.py created to replace functionality
#    within deployAdminConsole.ant config action.
#
#------------------------------------------------------------------------------
import sys

lineSep = java.lang.System.getProperty('line.separator')

# Get the directory, as a string, where WAS is installed.
#
# The WAS install directory should be specified in the WAS_INSTALL_ROOT
# variable, so we check there first.  If for some reason that's not defined,
# we'll try the WAS_HOME System property.  However, that value is useless
# if we're connected to a DMgr through the wsadmin of a federated node.
def getWASHome(cell, node):
	varMap = AdminConfig.getid("/Cell:" + cell + "/Node:" + node + "/VariableMap:/")
	entries = AdminConfig.list("VariableSubstitutionEntry", varMap)
	eList = entries.split(lineSep)
	for entry in eList:
		name =  AdminConfig.showAttribute(entry, "symbolicName")
		if name == "WAS_INSTALL_ROOT":
			value = AdminConfig.showAttribute(entry, "value")
			return value
	#failover
	return java.lang.System.getenv('WAS_HOME')


# Get the WAS systemApps directory.
#
# The WAS systemApps directory is always located at <WAS_HOME>/systemApps 
def getSystemAppsDir(cell, node):
	fileSep = getFileSep(node)
	return getWASHome(cell, node) + fileSep + "systemApps"

# Get the directory, as a string, of the isclite.ear application.
#
# The isclite.ear is located in <WAS_HOME>/systemApps/isclite.ear
def getISCDir(cell, node):
	fileSep = getFileSep(node)
	return getSystemAppsDir(cell, node) + fileSep + "isclite.ear"

# Get the file separator character
#
# This gets the file separator for the node in which we plan to install the
# console.  Therefore we can't just use the value that python on Java gives
# us.  Instead, check the platform of the node, and use "\" for windows and
# "/" for everything else.
def getFileSep(node): 
	os = AdminTask.getNodePlatformOS("-nodeName " + node)
	if os == 'windows':
		return '\\'
	else:
		return '/'

#------------------------------------------------------------------------------
# setupIEHSClassloader 
# Set the classloader for IEHS to PARENT_LAST
#------------------------------------------------------------------------------
def setupIEHSClassloader():
	print "Setting IEHS classloader to PARENT_LAST"
	isclite = AdminConfig.getid("/Deployment:isclite/")
	iscliteDepObject = AdminConfig.showAttribute(isclite, "deployedObject")
	modules =  AdminConfig.list("WebModuleDeployment", iscliteDepObject).split(lineSep)
	for module in modules:
		if AdminConfig.showAttribute(module, "uri") == "iehs.war":
			AdminConfig.modify(module, [['classloaderMode', 'PARENT_LAST']])
			return 1
	#return 0 for failure
	return 0	

#------------------------------------------------------------------------------
# updateDeploymentXml 
#------------------------------------------------------------------------------
def updateDeploymentXml():
	try:
		print "Updating deployment.xml"
		isclite = AdminConfig.getid("/Deployment:isclite/")
		iscliteDepObject = AdminConfig.showAttribute(isclite, "deployedObject")
		prop = [['name', 'com.ibm.ws.classloader.sharedLibBypass'], ['value', 'true'], ['required', 'false']]
		AdminConfig.create("Property", iscliteDepObject, prop)
		return 1
	except:
		print "Error during updateDeploymentXml():", sys.exc_info()
		return 0	


#------------------------------------------------------------------------------
# setCellVar 
# Set a WAS substitution variable with the name WAS_CELL_NAME, containing the
# name of the current cell.  It's needed in iehs.properties to locate help
# content.
#------------------------------------------------------------------------------
def setCellVar(cell):
	try:
		print "Setting WAS_CELL_NAME variable"
		return 1
	except:
		print "Error during setCellVar(" + cell + "):", sys.exc_info()
		return 0

#------------------------------------------------------------------------------
# deployAdminConsole
# Deploy the isclite.ear using the AdminApp install command, and then map it
# to the admin_host virtual host.
#------------------------------------------------------------------------------
def deployAdminConsole(cell, node, server):
	iscDir  = getISCDir(cell, node)
	sysAppDir  = getSystemAppsDir(cell, node)
	try:
		print "Deploying isclite.ear"
		appListString = AdminApp.list("WebSphere:cell=" + cell + ",node=" + node + ",server=" + server)
		appList = appListString.splitlines()
		if "isclite" in appList:
			print "Admin Console is already installed."
			return 0
		AdminApp.install(iscDir, ['-node', node, '-server', server, '-appname', 'isclite', '-usedefaultbindings', '-copy.sessionmgr.servername', server, '-zeroEarCopy', '-skipPreparation', '-installed.ear.destination', '$(WAS_INSTALL_ROOT)/systemApps'])
		
		#Do virtual host mapping
		print "Mapping isclite to admin_host"
		AdminApp.edit('isclite', ['-MapWebModToVH', [['.*', '.*', 'admin_host']]])
	except:
		print "Exception occurred during deployAdminConsole(" + cell +", " + node + ", " + server + "):", sys.exc_info()
		return 0
	return 1


#------------------------------------------------------------------------------
# Main entry point
#------------------------------------------------------------------------------

cell = None
node = None
server = None

# Try to get a node, cell, and server from command-line arguments
i = 0
while i < len(sys.argv):
	if sys.argv[i] == "-server": 
		server = sys.argv[i + 1]
		i = i + 1
	elif sys.argv[i] == "-node":
		node = sys.argv[i + 1]
		i = i + 1	
	elif sys.argv[i] == "-cell":
		cell = sys.argv[i + 1]
		i = i + 1	
	i = i + 1

if cell == None:
	print "Please specify a cell with the \'-cell\' option when in disconnected mode"
	sys.exit(100)
elif node == None:
	print "Please specify a node with the \'-node\' option when in disconnected mode"
	sys.exit(101)
elif server == None:
	print "Please specify a server with the \'-server\' option."
	sys.exit(102)
else:
	print "Cell:  " + cell + ", Node:  " + node + ", Server:  " + server
	
if server != None and cell != None and node != None:
	retVal = deployAdminConsole(cell, node, server)
	if retVal == 1:
		retVal = updateDeploymentXml()
	else:
		sys.exit(105)
	if retVal == 1:
		retVal = setCellVar(cell)
	else:
		sys.exit(106)
	if retVal == 1:
		retVal = setupIEHSClassloader()
	else:
		sys.exit(107)
	if retVal == 1:
		AdminConfig.save()
	else:
		print "Skipping Config Save"
		sys.exit(108)
