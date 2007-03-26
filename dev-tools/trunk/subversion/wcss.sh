#! /bin/bash
#set -x

## wcss: [svn] working copy snapshot
##
## Use cases:
##      1) save state of locally modified files
##      2) Above 1) plus clearing the local state, to prepare for a merge
##      3) to restore state

## Global variables
SNAPSHOT_PREFIX="svnsnapshot"
SNAPSHOT_PREFIX_SEPARATOR="-"
SNAPSHOT_DIRECTORY="${TMPDIR:-/tmp}"

## Function definitions
wcss_usage() {
    cat 1>&2 << __USAGE__

    Usage: $0 [options]

    Options:
        -l          lists available snapshots
        -s [name]   saves a snapshot with a default name of 'date +%Y_%m_%d_%H:%M:%S'
        -r <name>   restore the snapshot, the snapshot name is required
        -d <name>   delete the saved snapshot, the snapshot name is required
    
__USAGE__
}

stdout() {
    echo $*
}

stderr() {
    echo 1>&2 $*
}

wcss_save() {
    snapshot="${1:?Snapshot required}"
    if test -f "${snapshot}"; then
        stderr "Snapshot ${snapshot} already exists, will not overwrite"
        return 1
    else
        if ! svn status | sed -e 's/^[^ 	]*//' -e 's/^ *//' | xargs tar cf "${snapshot}"; then
            stderr "Unable to create working copy snapshot"
            return 1
        else
            stdout "Saved working copy snapshot in: ${snapshot}"
        fi
    fi
}

wcss_restore() {
    snapshot="${1:?Snapshot required}"
    if test -f "${snapshot}"; then
        stdout "Restoring snapshot..."
        tar xvf "${snapshot}"
        return $?
    else
        stderr "Snapshot does not exist, expected to find '${snapshot}'"
        return 1
    fi
}

wcss_delete() {
    snapshot="${1:?Snapshot required}"
    if test -f "${snapshot}"; then
        stdout "Removing ${snapshot} ..."
        rm "${snapshot}"
        return $?
    else
        stderr "Cannot delete non-existent snapshot"
    fi
}

wcss_list() {
    cd "${SNAPSHOT_DIRECTORY}"
    for snapshot in $(ls | grep "^${SNAPSHOT_PREFIX}${SNAPSHOT_PREFIX_SEPARATOR}.*\\.tar\$"); do
        snapshot_name="$(echo ${snapshot} | sed -e "s/^${SNAPSHOT_PREFIX}${SNAPSHOT_PREFIX_SEPARATOR}//" -e 's/.tar$//')"
        stdout "${snapshot_name}"
    done
    cd "${OLDPWD}"
}

## Main program
snapshot_name="$(date +%Y_%m_%d_%H:%M:%S)"
list="no"
save="no"
restore="no"
delete="no"
while getopts lsr:d: option; do
    case "${option}" in
        l) list="yes";;
        s) save="yes";;
        r) restore="yes"
           snapshot_name="${OPTARG:?Snapshot name is required}";;
        d) delete="yes"
           snapshot_name="${OPTARG:?Snapshot name is required}";;
        ?) wcss_usage
           exit 2;;
    esac
done

## -s takes an optional argument, see if they used it or not
shift $((${OPTIND}-1))
if test "${save}" = "yes" -a -n "${1}"; then
    snapshot_name="${1}"
fi

snapshot_path="${SNAPSHOT_DIRECTORY}/${SNAPSHOT_PREFIX}${SNAPSHOT_PREFIX_SEPARATOR}${snapshot_name}.tar"
if test "${save}" = "yes"; then
    wcss_save "${snapshot_path}"
    if ! test "$?" = "0"; then
        stderr "Unable to save snapshot ${snapshot_name}"
        exit 1
    fi
fi

if test "${restore}" = "yes"; then
    wcss_restore "${snapshot_path}"
    if ! test "$?" = "0"; then
        stderr "Unable to restore snapshot ${snapshot_name}"
        exit 1
    fi
fi

if test "${delete}" = "yes"; then
    wcss_delete "${snapshot_path}"
    if ! test "$?" = "0"; then
        stderr "Unable to delete snapshot ${snapshot_name}"
        exit 1
    fi
fi

if test "${list}" = "yes"; then
    wcss_list
    if ! test "$?" = "0"; then
        stderr "Unable to list snapshots"
        exit 1
    fi
fi
