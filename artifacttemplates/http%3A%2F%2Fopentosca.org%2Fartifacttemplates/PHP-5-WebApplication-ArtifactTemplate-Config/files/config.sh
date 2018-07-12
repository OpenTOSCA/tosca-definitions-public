#!/bin/bash
echo "### Configure the PHP WebApplication by injecting the provisioning date into the index.php" >> ~/management.log 2>&1;
sudo sed -i "s|//REPLACEME|echo(\"I was provisioned automatically!!!\");|g" /var/www/html/index.php;
