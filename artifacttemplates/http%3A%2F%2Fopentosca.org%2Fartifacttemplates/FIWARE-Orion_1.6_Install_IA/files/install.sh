#!/bin/sh
sudo /bin/sh -c "echo '127.0.0.1' $(hostname) >> /etc/hosts";
sudo apt-get -y -q update > /dev/null;
echo "### Installing dependencies for Orion Context Broker !!!"
echo "### Install Building tools"
sudo apt-get -y -q install make cmake g++ scons > /dev/null;
echo "\n\n### Install required libraries\n"
sudo apt-get -y -q install libboost-all-dev > /dev/null;
sudo apt-get -y -q install libcurl4-gnutls-dev > /dev/null;
sudo apt-get -y -q install uuid-dev > /dev/null;
echo "### Install Mongo Driver from source"
wget --quiet https://github.com/mongodb/mongo-cxx-driver/archive/legacy-1.0.7.tar.gz > /dev/null;
tar xfvz legacy-1.0.7.tar.gz > /dev/null;
cd mongo-cxx-driver-legacy-1.0.7;
scons -Q > /dev/null;
sudo scons install -Q --prefix=/usr/local > /dev/null;
cd ..;
echo "### Install rapidjson from source"
wget --quiet https://github.com/miloyip/rapidjson/archive/v1.0.2.tar.gz;
tar xfz v1.0.2.tar.gz;
sudo mv rapidjson-1.0.2/include/rapidjson/ /usr/local/include;
echo "### Install libmicrohttpd from source"
sudo apt-get -y -q install libmicrohttpd-dev;
echo "### Installing Orion Context Broker !!!"
sudo apt-get -y -q install git;
sudo git clone -b release/1.6.0 -q https://github.com/telefonicaid/fiware-orion;
cd fiware-orion;
sudo make --silent;
sudo make --silent install INSTALL_DIR=/usr > /dev/null;