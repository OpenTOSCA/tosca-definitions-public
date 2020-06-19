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
  if [[ "${entry[1]}" == *.py ]]; then
    echo "Found DA python script"
    echo $csarRoot${entry[1]}
    # the -u forces the output to be flushed continuesly, nohup is empty without
    nohup python -u $csarRoot${entry[1]} $Condition > ~/nohup.out;
  fi
done
