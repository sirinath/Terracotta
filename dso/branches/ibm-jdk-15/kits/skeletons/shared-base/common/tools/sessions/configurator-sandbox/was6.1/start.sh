#! /bin/sh

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
    _deployWars "${port}" "${binDir}/${port}/webapps"
    if test "$?" != "0"; then
        _error unable to deploy web applications to WebSphere Application Server on port "${port}"
        _stopWebSphere "${port}"
        exit 1
    else
        _runWsAdmin "${port}" "${binDir}/wait-for-shutdown.py"
        exit $?
    fi
fi
