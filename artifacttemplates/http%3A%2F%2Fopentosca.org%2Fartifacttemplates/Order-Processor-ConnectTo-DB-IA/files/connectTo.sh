#!/bin/bash

# create config file
FILE=~/$CSAR/order-processor.conf
if [ ! -e "$FILE" ] ; then
    touch $FILE
fi

# add env vars to connect to the database
echo "SPRING_DATASOURCE_URL=jdbc:mysql://${TARGET_VMIP}:${TARGET_DBMSPort}/${TARGET_DBName}" >> $FILE
echo "SPRING_DATASOURCE_DRIVERCLASSNAME=com.mysql.jdbc.Driver" >> $FILE
echo "SPRING_DATASOURCE_USERNAME=$TARGET_DBUser" >> $FILE
echo "SPRING_DATASOURCE_PASSWORD=$TARGET_DBPassword" >> $FILE
echo "SPRING_JPA_DATABASEPLATFORM=org.hibernate.dialect.MySQL5Dialect" >> $FILE