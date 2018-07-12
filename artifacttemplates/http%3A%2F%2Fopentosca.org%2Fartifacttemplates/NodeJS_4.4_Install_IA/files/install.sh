#!/bin/sh

curl -sL https://deb.nodesource.com/setup_4.x | sudo -E bash -;
sudo apt-get install -y nodejs;

sudo apt-get install -y build-essential;

ln -s /usr/bin/nodejs /usr/bin/node 
