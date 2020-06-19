#!/bin/bash
# Start Master
sudo ./spark/sbin/start-master.sh
# Start Worker
sudo ./spark/sbin/start-slave.sh spark://127.0.1.1:7077