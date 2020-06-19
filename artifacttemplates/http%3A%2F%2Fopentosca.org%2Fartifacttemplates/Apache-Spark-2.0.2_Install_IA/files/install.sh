#!/bin/bash
cd ubuntu
# Download
wget http://ftp-stud.hs-esslingen.de/pub/Mirrors/ftp.apache.org/dist/spark/spark-2.0.2/spark-2.0.2-bin-hadoop2.7.tgz
# Decompress
tar -xvf spark-2.0.2-bin-hadoop2.7.tgz
sudo chmod 755 spark-2.0.2-bin-hadoop2.7; #FIXME
# Create softlink
ln -s spark-2.0.2-bin-hadoop2.7 spark;