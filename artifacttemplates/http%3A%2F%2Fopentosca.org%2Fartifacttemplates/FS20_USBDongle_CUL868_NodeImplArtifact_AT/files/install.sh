#!/bin/bash
installed=$(lsusb | grep Atmel)

if [ "$installed" != "" ]; then
	echo "firmware already installed"
	exit 0
fi

#find csar root
csarRoot=$(find ~ -maxdepth 1 -path "*.csar");

IFS=';' read -ra NAMES <<< "$DAs";
for i in "${NAMES[@]}"; do
	echo "KeyValue-Pair: "
    	echo $i
	IFS=',' read -ra entry <<< "$i";    
	echo "Key: "
    	echo ${entry[0]}
    	echo "Value: "
    	echo ${entry[1]}
	if [[ "${entry[1]}" == *culfw-1.67.tar.gz ]];
	then

	sudo apt-get update
	sudo apt-get install dfu-programmer build-essential
	tar xvfz $csarRoot${entry[1]}
	cd culfw-1.67/Devices/CUL/
	sudo make usbprogram_v3
	fi
done