#!/bin/bash
echo "### Starting the apache 2" >> ~/management.log 2>&1;
sudo service apache2 start >> ~/management.log 2>&1;