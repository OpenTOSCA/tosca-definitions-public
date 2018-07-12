#!/bin/sh
# Parameters:
# $ContainerIP
# $Port

csarRoot=$(find ~ -maxdepth 1 -path "*.csar")
echo $csarRoot

echo "$ContainerIP:$Port" >> $csarRoot/tinytodo_connections.txt;
echo $(cat $csarRoot/tinytodo_connections.txt)
