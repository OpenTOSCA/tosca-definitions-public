#!/bin/bash
CSARROOT=$(find ~ -maxdepth 1 -path "*.csar");
IFS=';' read -ra FILES <<< "$DAs"
for i in "${FILES[@]}"; do
    IFS=',' read -ra ENTRY <<< "$i"
    if [[ ( -f $CSARROOT${ENTRY[1]} ) && ( ${ENTRY[1]} == *.war ) ]]; then
        if [[ ${ENTRY[1]} == *ROOT.war ]]; then
            sudo rm -rf /opt/tomcat/webapps/ROOT*
        fi
        sudo cp $CSARROOT${ENTRY[1]} /opt/tomcat/webapps
    fi
done
sleep 30
