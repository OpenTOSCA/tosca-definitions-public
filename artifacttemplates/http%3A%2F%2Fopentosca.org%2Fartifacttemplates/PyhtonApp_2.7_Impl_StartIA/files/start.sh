#!/bin/bash

#find csar root
csarRoot=$(find ~ -maxdepth 1 -path "*.csar");

arguments=$(env)

IFS=';' read -ra NAMES <<< "$DAs";
for i in "${NAMES[@]}"; do
	echo "KeyValue-Pair: "
    	echo $i
	IFS=',' read -ra entry <<< "$i";
	echo "Key: "
    	echo ${entry[0]}
    	echo "Value: "
    	echo ${entry[1]}
	if [[ "${entry[1]}" == *.py ]];
	then
	# the -u forces the output to be flushed continuesly, nohup is empty without
	nohup python -u $csarRoot${entry[1]} $arguments > ~/nohup.out &
	sleep 1
	fi
done
