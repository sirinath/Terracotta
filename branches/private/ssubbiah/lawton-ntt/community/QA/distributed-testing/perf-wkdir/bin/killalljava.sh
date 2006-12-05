#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

cd $(dirname $0)
. assign-hosts.sh

for i in $ALL_HOSTS
do
		ssh $i "killall -9 java; killall -9 vmstat" &
done
wait

