#!/bin/bash

#find csar root
csarRoot=$(find ~ -maxdepth 1 -path "*.csar");

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
	
	userDir="$(/usr/bin/dirname "$csarRoot")"
	flinkBinDir=$userDir/flink/bin
	echo "Bin dir of Flink: " $flinkBinDir
    # invoke flink with DA as parameter
	$flinkBinDir/pyflink2.sh $csarRoot${entry[1]}
	
	fi
done
