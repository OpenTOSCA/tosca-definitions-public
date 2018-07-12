#!/bin/bash
echo "### Removing libapache2-mod-php5" >> /home/ubuntu/install.log 2>&1;
sudo apt-get -qqy remove libapache2-mod-php5 >> /home/ubuntu/install.log 2>&1;