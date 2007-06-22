#!/bin/bash
if [ "$#" -lt "2" ]; then
  echo "Usage: $0 <branch> <revision> [commit [message]]"
  exit 1;
fi
branch="$1"
rev="$2"
commit="$3"
optional_msg="$4"
if [ "$commit" != "" ]; then
  echo "================================================================================"
  echo "Running: svn commit -m '${optional_msg+${optional_msg}: }merge -c ${rev} https://svn.terracotta.org/repo/tc/dso/branches/${branch}'"
  echo "================================================================================"
  exec svn commit -m "${optional_msg+${optional_msg}: }merge -c ${rev} https://svn.terracotta.org/repo/tc/dso/branches/${branch}"
else
  echo "================================================================================"
  echo "Running: svn merge -c ${rev} https://svn.terracotta.org/repo/tc/dso/branches/${branch}"
  echo "================================================================================"
  exec  svn merge -c ${rev} https://svn.terracotta.org/repo/tc/dso/branches/${branch}
fi

