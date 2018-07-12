#!/bin/bash
sudo sh -c "echo '127.0.0.1' $(hostname) >> /etc/hosts";
sudo apt-get -qq update;
#sudo apt-get -qq -y upgrade;
sudo apt-get install -qq -y python3-pip;
#sudo pip3 install aiohttp_cors; # workaround since is currently (16.08.17 0.51.2) not working without it
#sudo pip3 install sqlalchemy;
sudo pip3 install paho-mqtt; # dependency if it will use an external mqtt broker
sudo pip3 install --upgrade homeassistant;