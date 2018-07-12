#!/bin/bash

# InputParameters:
# $Condition
# $VMIP

csarRoot=$(find ~ -maxdepth 1 -path "*.csar")

echo "Condition: " >> $csarRoot/EPLQuery.log;
echo $Condition >> $csarRoot/EPLQuery.log;

# parse condition
IFS=';' read -ra EPLStatements <<< "$Condition";
for i in "${EPLStatements[@]}"; do
  echo "EPL Statement: " >> $csarRoot/EPLQuery.log;
  bodyContent="{\"query\": \"$i\"}"
  echo $bodyContent >> $csarRoot/EPLQuery.log;
    # send HTTP request to push statement to esper
  curl -X POST -H "Accept: application/json" -H "Content-Type: application/json" http://$VMIP:8080/EsperService/queries --data ''"$bodyContent"'' >> $csarRoot/EPLQuery.log;
  sleep 3
done
