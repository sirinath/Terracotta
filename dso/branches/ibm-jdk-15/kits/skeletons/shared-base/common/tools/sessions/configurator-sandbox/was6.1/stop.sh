#! /bin/sh

###########################################################################################
##
## Main program
##
##    Arguments: [-debug] <port>
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

_info stopping WebSphere Application Server on port "${port}"...
_stopWebSphere "${port}"
exit $?
