#! /bin/sh
set -x

###########################################################################################
##
## Main program
##
##    Arguments: [-debug] <port> [nodso]
##
###########################################################################################

if test "${1}" = "-debug"; then
    shift
    set -x
fi

binDir="`dirname $0`"
port="${1:?You must specify a port as the first argument}"

. "${binDir}/websphere-common.sh"

if ! _validateWasHome; then
    _error WAS_HOME must point to a valid WebSphere Application Server 6.1 installation
    exit 1
fi

if ! _createProfile "${port}"; then
    _error Unable to create a profile 'for' port "${port}" to run the Terracotta Configurator
    exit 1
fi

_info starting WebSphere Application Server on port "${port}"...
_startWebSphere "${port}" "${2}"
if test "$?" != "0"; then
    _error unable to start WebSphere Application Server on port "${port}"
    _stopWebSphere "${port}"
    exit 1
else
    # Hot deploy the .war files in the webapps directory
    for war in "${binDir}/${port}"/webapps/*.war; do
        _warn Not hot deploying WAR "${war}" because Nat is not finished yet
    done
    # WebSphere starts in the background, we call a script to monitor its state, and
    # exit when WebSphere exits
    _deployWars "${port}" "${binDir}/${port}/webapps" "${binDir}/deploy-and-wait.py"
    exit $?
fi
