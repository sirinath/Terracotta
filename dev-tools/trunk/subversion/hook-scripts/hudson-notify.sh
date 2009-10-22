# Notify Hudson of changes

REPOS="$1"
REV="$2"
HUDSON_MASTER_HOST="su10vmo3:9000"

UUID=`svnlook uuid $REPOS`
/usr/bin/wget \
  --header "Content-Type:text/plain;charset=UTF-8" \
  --post-data "`svnlook changed --revision $REV $REPOS`" \
  --output-document "-" \
  http://$HUDSON_MASTER_HOST/hudson/subversion/${UUID}/notifyCommit?rev=$REV
