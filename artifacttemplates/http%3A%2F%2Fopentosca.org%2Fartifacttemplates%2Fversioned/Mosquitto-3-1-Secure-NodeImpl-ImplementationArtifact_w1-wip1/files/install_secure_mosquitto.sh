#!/bin/sh
# input parameters
# $VMIP
# output certificate
# $Certificate
echo $VMIP;
apt-get install -y python-software-properties;
apt-add-repository -y ppa:mosquitto-dev/mosquitto-ppa;
sudo apt-get update -qq;
sudo apt-get install -y mosquitto;
# Configuring certificates
sudo apt-get install -y openssl;
#openssl genrsa -des3 -out ca.key 2048 #generated password protected
openssl genrsa -out ca-smartorchestra.key 2048;

openssl req -new -x509 -days 1826 -key ca-smartorchestra.key -out ca-smartorchestra.crt -subj "/C=DE/ST=Berlin/L=Berlin/O=SmartOrchestra/OU=IT Department/CN=$VMIP";

openssl genrsa -out smartorchestra-server.key 2048;


openssl req -new -out smartorchestra-server.csr -key smartorchestra-server.key -subj "/C=DE/ST=BW/L=Stuttgart/O=University of Stuttigart/OU=IPVS-IAAS/CN=$VMIP";

openssl x509 -req -in smartorchestra-server.csr -CA ca-smartorchestra.crt -CAkey ca-smartorchestra.key -CAcreateserial -out smartorchestra-server.crt -days 360;
#mosquitto_sub -h 192.168.209.181 -t test --cafile ca.crt -p 8883 --tls-version tlsv1
#mosquitto_pub -h 192.168.209.181 -t test --cafile ca.crt -p 8883 -m "bla" --tls-version tlsv1

# copying certificates to mosquitto
sudo mkdir /etc/mosquitto/ca_certificates;
sudo mkdir /etc/mosquitto/certs;
sudo cp ca-smartorchestra.crt /etc/mosquitto/ca_certificates/ca-smartorchestra.crt;
sudo cp smartorchestra-server.crt /etc/mosquitto/certs/smartorchestra-server.crt;
sudo cp smartorchestra-server.key /etc/mosquitto/certs/smartorchestra-server.key;

#editing mosquitto configuration file
sudo service mosquitto stop;
sudo echo "port 8883" >> /etc/mosquitto/mosquitto.conf
sudo echo "cafile /etc/mosquitto/ca_certificates/ca-smartorchestra.crt" >> /etc/mosquitto/mosquitto.conf
sudo echo "keyfile /etc/mosquitto/certs/smartorchestra-server.key" >> /etc/mosquitto/mosquitto.conf
sudo echo "certfile /etc/mosquitto/certs/smartorchestra-server.crt" >> /etc/mosquitto/mosquitto.conf
sudo echo "tls_version tlsv1" >> /etc/mosquitto/mosquitto.conf
sudo service mosquitto start;

echo "Certificate="$(cat /etc/mosquitto/ca_certificates/ca-smartorchestra.crt)
