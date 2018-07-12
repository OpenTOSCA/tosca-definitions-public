#!/bin/bash
echo "### Update Package List" >> ~/management.log 2>&1;
sudo apt-get -qqy update >> ~/management.log 2>&1;
echo "Available Packages for PHP 5 Apache2 Module:" >> ~/management.log 2>&1;
apt-cache policy libapache2-mod-php5 >> ~/management.log 2>&1;
#
echo "### Install libapache2-mod-php5" >> ~/management.log 2>&1;
sudo apt-get -qqy install libapache2-mod-php5 >> ~/management.log 2>&1;