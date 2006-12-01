##!/bin/sh
#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

TOPDIR=`dirname $0`/..
. ${TOPDIR}/libexec/lrt-functions.sh

if [ "${1}x" == "x" ]; then
  echo "Please specify the path to your repository"
  exit 1
fi

tc_dist="${repo}/code/base/build/dist/terracotta-2.1.0"
if [ -e repo ]; then
    echo "repo directory already exists.  Not making the symlink to $1."
else
    ln -s $1 repo
fi

pushd ${1}/code/base

echo "Building kit from `pwd`..."
../../buildsystems/bin/tcbuild dist dso
echo "Done building kit."

popd
if [ -e terracotta ]; then
    echo "terracotta directory already exists.  Not making the symlink to ${tc_dist}."
else
    ln -s ${tc_dist} terracotta
fi

ls -l
echo "To run the lrt, execute the following command:"
echo "${lrt_home}/bin/lrt.sh"
