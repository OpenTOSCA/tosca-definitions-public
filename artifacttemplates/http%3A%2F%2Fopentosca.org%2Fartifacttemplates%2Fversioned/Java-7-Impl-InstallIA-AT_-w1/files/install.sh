#!/bin/bash
sudo add-apt-repository -y ppa:openjdk-r/ppa
sudo apt-get -y -q update;
sudo apt-get -y -q install openjdk-7-jdk;

sudo update-java-alternatives -s java-1.7.0-openjdk-amd64;
echo "\n\n### Set JAVA_HOME";
sudo sh -c "echo '127.0.0.1' $(hostname) >> /etc/hosts";
sudo sh -c "echo 'JAVA_HOME=\"'$(readlink -f /usr/bin/java | sed "s:/bin/java::")'\"' >> /etc/environment";
export JAVA_HOME="$(readlink -f /usr/bin/java | sed "s:/bin/java::")";