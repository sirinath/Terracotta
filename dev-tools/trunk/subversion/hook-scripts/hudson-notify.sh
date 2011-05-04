#!/bin/sh

# Notify Hudson of changes

repos="$1"
rev="$2"
repo_name=`basename $repos`
hudson_master_host="rh5fm0.terracotta.lan:9000"
dso_hudson_master_host="su10mo9.terracotta.lan:9000"

uuid=`svnlook uuid $repos`
notify_url=http://$hudson_master_host/subversion/${uuid}/notifyCommit?rev=$rev
dso_notify_url=http://$dso_hudson_master_host/subversion/${uuid}/notifyCommit?rev=$rev
timeout=10
tries=3

for url in $notify_url $dso_notify_url; do
  /usr/bin/wget \
    --header "Content-Type:text/plain;charset=UTF-8" \
    --timeout $timeout --tries $tries \
    --post-data "`svnlook changed --revision $rev $repos`" \
    --output-document "-" \
    $url
done

timestamp=`/bin/date`
log=/export1/svn-mirror/logs/$repo_name-post-commit.log
echo "$timestamp: $notify_url" > $log
echo "post data $repos: `svnlook changed --revision $rev $repos`" >> $log
echo "">> $log

