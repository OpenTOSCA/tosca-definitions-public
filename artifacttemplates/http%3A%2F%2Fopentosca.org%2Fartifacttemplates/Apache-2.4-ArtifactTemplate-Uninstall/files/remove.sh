#!/bin/bash
echo "### Removing Apache-v2.4" >> /home/ubuntu/install.log 2>&1;
sudo apt-get -qqy remove apache2 >> /home/ubuntu/install.log 2>&1;