#!/bin/sh
# we need the openMTC host and topic as parameters IP and Name

csarRoot=$(find ~ -maxdepth 1 -path "*.csar")
echo $csarRoot
echo "$Name = $IP" > $csarRoot/connections.txt;
echo $(cat $csarRoot/connections.txt)

