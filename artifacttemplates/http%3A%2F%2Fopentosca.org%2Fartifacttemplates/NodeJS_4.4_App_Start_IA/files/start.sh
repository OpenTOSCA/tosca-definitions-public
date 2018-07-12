#!/bin/bash

#find csar root
csarRoot=$(find ~ -maxdepth 1 -path "*.csar");

IFS=';' read -ra NAMES <<< "$DAs";
for i in "${NAMES[@]}"; do
        #echo "KeyValue-Pair: "
        #echo $i
        IFS=',' read -ra PATH <<< "$i";    
        #echo "Key: "
        #echo ${PATH[0]}
        #echo "Value: "
        #echo ${PATH[1]}
        if [[ "${PATH[1]}" == *.js ]];
        then    
        /usr/bin/nohup /usr/bin/node $csarRoot${PATH[1]} Port=$Port &
        fi
done





