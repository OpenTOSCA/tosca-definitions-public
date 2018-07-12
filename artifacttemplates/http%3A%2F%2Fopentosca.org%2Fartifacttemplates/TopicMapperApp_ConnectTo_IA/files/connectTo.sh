#!/bin/sh
# Parameters:
# $EntityID: 
# $AttributeName: 
# $Host: host of orion context broker
# $Port: 
# $ChannelType: IN or OUT, which indicates the topic is to be subscribed(IN) or publish on (OUT)
csarRoot=$HOME/$CSAR
echo $csarRoot

echo "$EntityID/attrs/$AttributeName/value = $Host:$Port = $ChannelType" >> $csarRoot/orion_connections.txt;
echo $(cat $csarRoot/orion_connections.txt)
