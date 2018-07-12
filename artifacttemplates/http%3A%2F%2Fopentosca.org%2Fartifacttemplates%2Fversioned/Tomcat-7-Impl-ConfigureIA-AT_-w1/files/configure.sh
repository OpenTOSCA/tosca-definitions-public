#!/bin/bash

#find csar root
csarRoot=$(find ~ -maxdepth 1 -path "*.csar");
#set JAVA_HOME
echo "JAVA_HOME=$JAVA_HOME" >> /etc/default/tomcat7;

echo "Got the following DAs:";
IFS=';' read -ra NAMES <<< "$DAs";
for i in "${NAMES[@]}"; do
        #echo "KeyValue-Pair: "
        #echo $i
        IFS=',' read -ra PATH <<< "$i";    
        #echo "Key: "
        #echo ${PATH[0]}
        #echo "Value: "
        #echo ${PATH[1]}
	echo ${PATH[1]}
        if [[ "${PATH[1]}" == *tomcat-users.xml ]];
        then    
		echo "Copy of tomcat-users.xml"
	        /usr/bin/sudo /bin/cp $csarRoot${PATH[1]} /var/lib/tomcat7/conf
        fi
	if [[ "${PATH[1]}" == *server.xml ]];
        then    
		echo "Copy of server.xml"
	        /usr/bin/sudo /bin/cp $csarRoot${PATH[1]} /var/lib/tomcat7/conf
        fi
done

/usr/bin/sudo service tomcat7 restart
