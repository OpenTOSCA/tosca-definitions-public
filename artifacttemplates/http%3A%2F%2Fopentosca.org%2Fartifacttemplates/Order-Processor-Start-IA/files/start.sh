#!/bin/bash

# export env vars from config file if present
FILE=~/$CSAR/order-processor.conf
if test -f "$FILE"; then
	set -o allexport
	source $FILE
	set +o allexport
fi

# search and start the order processor
CSARROOT=$(find ~ -maxdepth 1 -path "*.csar");
IFS=';' read -ra FILES <<< "$DAs"
for i in "${FILES[@]}"; do
    IFS=',' read -ra ENTRY <<< "$i"
    if [[ ( -f $CSARROOT${ENTRY[1]} ) && ( ${ENTRY[1]} == *.jar ) ]]; then
        if [[ ${ENTRY[1]} == *order-processor.jar ]]; then
            /usr/bin/nohup java -jar $CSARROOT${ENTRY[1]} &
        fi
    fi
done
sleep 5
exit 0
