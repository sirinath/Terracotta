#!/bin/sh

# Notify Jenkins master of changes

repos="$1"
rev="$2"
repo_name=`basename $repos`
jenkinsmaster="jenkinsmaster.terracotta.lan:9000"

uuid=`svnlook uuid $repos`
notify_url=http://$jenkinsmaster/subversion/${uuid}/notifyCommit?rev=$rev
timeout=10
tries=3

log=/export2/svn-mirror/logs/$repo_name-post-commit.log
echo "" > $log

for url in $notify_url; do
  /usr/bin/wget \
    --header "Content-Type:text/plain;charset=UTF-8" \
    --timeout $timeout --tries $tries \
    --post-data "`svnlook changed --revision $rev $repos`" \
    --output-document "-" \
    $url

  timestamp=`/bin/date`
  echo "$timestamp: $url" >> $log
  echo "post data $repos: `svnlook changed --revision $rev $repos`" >> $log
  echo "">> $log

done

