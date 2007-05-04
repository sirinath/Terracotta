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
    "${WAS_HOME}/bin/manageprofiles.sh" -listProfiles | grep -q "tc-${1}"
    if test "$?" != "0"; then
        _info creating profile "tc-${1}" 'for' port "${1}"...
        _info
        _info "	==> THIS CAN TAKE A LONG TIME SO PLEASE BE PATIENT <=="
        _info
        if ! "${WAS_HOME}/bin/manageprofiles.sh" -create -templatePath "${WAS_HOME}/profileTemplates/default" -portsFile "${binDir}/profiles/${1}.port-defs" -profileName "tc-${1}" -enableAdminSecurity false -isDeveloperServer; then
            _warn unable to create profile "tc-${1}" 'for' port "${1}"
            return 1
        fi
    else
        _info WebSphere profile "tc-${1}" already exists, skipping profile creation 'for' port "${1}"
    fi
}

function _runWsAdmin() {
    "${WAS_HOME}/bin/wsadmin.sh" -lang jython -profileName "tc-${1}" -javaoption -DprofileName="${1}" -f "${2}"
    return $?
}

function _deployWars() {
    for war in "${2}"/*.war; do
        _warn Not hot deploying WAR "${war}" because Nat is not finished yet
    done
    return $?
}

function _startWebSphere() {
    if test "${2}" != "nodso"; then
        # Instrument WebSphere for use with Terracotta
        _error Do not know how to instrument 'for' Terracotta just yet....
    fi
    "${WAS_HOME}/bin/startServer.sh" server1 -profileName "tc-${1}"
    return $?
}

function _stopWebSphere() {
    "${WAS_HOME}/bin/stopServer.sh" server1 -profileName "tc-${1}"
    return $?
}
