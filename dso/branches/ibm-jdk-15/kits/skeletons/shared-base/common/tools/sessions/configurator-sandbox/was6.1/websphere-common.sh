###########################################################################################
##
## Supporting functions
##
###########################################################################################

function _stdout() {
    echo "$@"
}

function _stderr() {
    echo 1>&2 "$@"
}

function _info() {
    _stdout ' [info] : ' "$@"
}

function _warn() {
    _stderr ' [warn] : ' "$@"
}

function _error() {
    _stderr '[error] : ' "$@"
}

function _executable() {
    test -r "${1}" -a -x "${1}"
    return $?
}

function _validateWasHome() {
    _info validating the WAS_HOME environment...
    if test -z "${WAS_HOME}"; then
        _warn WAS_HOME not defined
        return 1
    fi
    _was_bin="startServer.sh stopServer.sh wsadmin.sh manageprofiles.sh"
    for bin in ${_was_bin}; do
        if ! _executable "${WAS_HOME}/bin/${bin}"; then
            _warn unable to locate "\${WAS_HOME}/bin/${bin}"
        fi
    done
}

function _createProfile() {
    "${WAS_HOME}/bin/manageprofiles.sh" -listProfiles | grep -q "${1}"
    if test "$?" != "0"; then
        _info creating profile 'for' port "${1}"...
        _info
        _info "	==> THIS CAN TAKE A LONG TIME SO PLEASE BE PATIENT <=="
        _info
        if ! "${WAS_HOME}/bin/manageprofiles.sh" -create -templatePath "${binDir}/profiles/${1}" -profileName "${1}"; then
            _warn unable to create profile 'for' port "${port}"
            return 1
        fi
    else
        _info WebSphere profile already exists, skipping profile creation 'for' port "${1}"
    fi
}

function _deployWars() {
    "${WAS_HOME}/bin/wsadmin.sh" -lang jython -profileName "${1}" -javaoption -DprofileName="${1}" -javaoption -DwarDirectory="${2}" -f "${3}"
    return $?
}

function _startWebSphere() {
    if test "${2}" != "nodso"; then
        # Instrument WebSphere for use with Terracotta
        _error Do not know how to instrument 'for' Terracotta just yet....
    fi
    "${WAS_HOME}/bin/startServer.sh" server1 -profileName "${1}"
    return $?
}

function _stopWebSphere() {
    "${WAS_HOME}/bin/stopServer.sh" server1 -profileName "${1}"
    return $?
}
