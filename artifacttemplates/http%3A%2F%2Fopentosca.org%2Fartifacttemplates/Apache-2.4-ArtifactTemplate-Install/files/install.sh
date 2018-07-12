#!/bin/bash
echo "### Update Package List" >> ~/management.log 2>&1;
sudo apt-get -qqy update >> ~/management.log 2>&1;
echo "Available Packages for Apache-v2:" >> ~/management.log 2>&1;
apt-cache policy apache2 >> ~/management.log 2>&1;
#
echo "### Install Apache-v2.4" >> ~/management.log 2>&1;
sudo apt-get -qqy install apache2 >> ~/management.log 2>&1;