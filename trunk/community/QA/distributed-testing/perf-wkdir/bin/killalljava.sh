cd $(dirname $0)
. assign-hosts.sh

for i in $ALL_HOSTS
do
		ssh $i "killall -9 java; killall -9 vmstat" &
done
wait

