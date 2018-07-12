#!/bin/bash

#find csar root
csarRoot=$(find ~ -maxdepth 1 -path "*.csar");

IFS=';' read -ra NAMES <<< "$DAs";
for i in "${NAMES[@]}"; do
  echo "KeyValue-Pair: "
  echo $i
  IFS=',' read -ra PATH <<< "$i";
  echo "Key: "
  echo ${PATH[0]}
  echo "Value: "
  echo ${PATH[1]}
  if [[ "${PATH[1]}" == *.py ]]; then
    echo "Found DA python script"
    echo $csarRoot${PATH[1]}
    # the -u forces the output to be flushed continuesly, nohup is empty without
    /usr/bin/nohup /usr/bin/python -u $csarRoot${PATH[1]} $Condition > ~/nohup.out;
  fi
done
