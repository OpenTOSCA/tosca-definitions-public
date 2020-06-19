#!/bin/bash

#find csar root
csarRoot=$(find ~ -maxdepth 1 -path "*.csar");

IFS=';' read -ra NAMES <<< "$DAs";
for i in "${NAMES[@]}"; do
        #echo "KeyValue-Pair: "
        #echo $i
        IFS=',' read -ra entry <<< "$i";    
        #echo "Key: "
        #echo ${entry[0]}
        #echo "Value: "
        #echo ${entry[1]}
        if [[ "${entry[1]}" == *.js ]];
        then    
        nohup node $csarRoot${entry[1]} Port=$Port &
        fi
done





