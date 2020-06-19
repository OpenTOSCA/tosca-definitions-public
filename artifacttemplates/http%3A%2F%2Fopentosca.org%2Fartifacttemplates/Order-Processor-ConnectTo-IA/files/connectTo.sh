#!/bin/bash

FILE=~/$CSAR/order-processor.conf

if [ ! -e "$FILE" ] ; then
    touch $FILE
fi

echo "CLOUD_AWS_REGION_STATIC=$AWSRegion" >> $FILE
echo "CLOUD_AWS_CREDENTIALS_ACCESSKEY=$AWSAccessKey" >> $FILE
echo "CLOUD_AWS_CREDENTIALS_SECRETKEY=$AWSSecretKey" >> $FILE
echo "DEMO_QUEUE=$TARGET_QueueName" >> $FILE