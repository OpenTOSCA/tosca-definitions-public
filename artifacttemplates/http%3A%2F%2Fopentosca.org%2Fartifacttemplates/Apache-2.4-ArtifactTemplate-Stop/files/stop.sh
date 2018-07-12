#!/bin/bash
echo "### Stopping the apache 2" >> ~/management.log 2>&1;
sudo service apache2 stop >> ~/management.log 2>&1;