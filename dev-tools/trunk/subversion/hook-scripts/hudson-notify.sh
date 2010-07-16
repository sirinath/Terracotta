# Notify Hudson of changes

repos="$1"
rev="$2"
repo_name=`basename $repos`
hudson_master_host="rh5fm0.terracotta.lan:9000"

uuid=`svnlook uuid $repos`
notify_url=http://$hudson_master_host/subversion/${uuid}/notifyCommit?rev=$rev
timeout=10
tries=3

/usr/bin/wget \
  --header "Content-Type:text/plain;charset=UTF-8" \
  --timeout $timeout --tries $tries \
  --post-data "`svnlook changed --revision $rev $repos`" \
  --output-document "-" \
  $notify_url

timestamp=`/bin/date`
log=$HOME/logs/$repo_name-post-commit.log
echo "$timestamp: $notify_url" > $log
echo "post data $repos: `svnlook changed --revision $rev $repos`" >> $log
echo "">> $log

