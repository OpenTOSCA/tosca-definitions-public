#!/bin/sh
#apt-get install -y python-software-properties;
#apt-add-repository -y ppa:mosquitto-dev/mosquitto-ppa;
sudo apt-get update -qq;
sudo apt-get install -y mosquitto;
sed -i "/^log_dest none/c\log_dest syslog" /etc/mosquitto/mosquitto.conf
sed -i "/#log_type error/c\log_type error" /etc/mosquitto/mosquitto.conf
sed -i "/#log_type warning/c\log_type warning" /etc/mosquitto/mosquitto.conf
sed -i "/#log_type notice/c\log_type notice" /etc/mosquitto/mosquitto.conf
sed -i "/#log_type information/c\log_type information" /etc/mosquitto/mosquitto.conf
sed -i "/#connection_messages true/c\connection_messages true" /etc/mosquitto/mosquitto.conf
sed -i "/#log_timestamp true/c\log_timestamp true" /etc/mosquitto/mosquitto.conf

