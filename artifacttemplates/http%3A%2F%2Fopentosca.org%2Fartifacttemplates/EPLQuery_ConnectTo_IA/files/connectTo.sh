#!/bin/bash
# Parameters:
# $EntityID: 
# $AttributeName: 
# $Host: host of orion context broker
# $Port: 
# $ChannelType: IN or OUT, which indicates the topic is to be subscribed(IN) or publish on (OUT)
# 
# $FIWARE-Service
# $VMIP

csarRoot=$(find ~ -maxdepth 1 -path "*.csar");
#echo $csarRoot;

#echo "$EntityID/attrs/$AttributeName/value = $Host:$Port = $ChannelType" >> $csarRoot/orion_connections.txt;
#echo $(cat $csarRoot/orion_connections.txt);

# if it contains already topic in und topic out, do start input and output adapters
if [ $ChannelType = "IN" ]; then

bodyContent="{\"protocol\":\"HTTP-Orion\",\"endpoint\":\"http://$Host:$Port/v2/entities\",\"topics\":[\"$EntityID/attrs/$AttributeName/value\"]}"
echo $bodyContent >> $csarRoot/EPLQuery.log;

# send HTTP request to add data source to esper
curl -X POST -H "Accept: application/json" -H "Content-Type: application/json" http://localhost:8080/EsperService/datasources --data ''"$bodyContent"'' >> $csarRoot/EPLQuery.log;

elif [ $ChannelType = "OUT" ]; then

bodyContentOUT="{\"protocol\":\"HTTP-Orion\",\"endpoint\":\"http://$Host:$Port/v2/entities\",\"topics\":[\"$EntityID/attrs/$AttributeName/value\"]}"
echo $bodyContentOUT >> $csarRoot/EPLQuery.log;

#get all queries
response=$(curl -X GET -H "Accept: text/plain" http://localhost:8080/EsperService/queries)
echo "get all queries: $response" >> $csarRoot/EPLQuery.log;

# parse condition
IFS=$';' read -ra query_ids <<< "$response";
for i in "${query_ids[@]}"; do
  echo "query_id: $i" >> $csarRoot/EPLQuery.log;
  
  # send HTTP request to add data sink (subscriber) to esper
  curl -X POST -H "Accept: application/json" -H "Content-Type: application/json" http://localhost:8080/EsperService/queries/$i/subscriptions --data ''"$bodyContentOUT"'' >> $csarRoot/EPLQuery.log;
  sleep 3
done


fi
