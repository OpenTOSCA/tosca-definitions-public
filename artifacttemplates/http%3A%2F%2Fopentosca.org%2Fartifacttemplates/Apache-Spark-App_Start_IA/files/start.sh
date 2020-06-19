#!/bin/bash

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
	if [[ "${entry[1]}" == *.py ]];
	then
	
	# User dir	
	userDir="$(/usr/bin/dirname "$csarRoot")"
	
	# DA location
	da_path=$csarRoot${entry[1]}
	echo "DA path: " $da_path 
	
	# DA file name
	da_file_name=$(/usr/bin/basename $da_path)
	echo "DA file name: " $da_file_name

	home_da_dir=$userDir/${entry[0]}
	echo "Home dir of DA: " $home_da_dir

	# Create DA directory in home
	mkdir -p $home_da_dir
	# Copy DA
	cp $da_path $home_da_dir
	
	# Spark bin dir		
	sparkBinDir=$userDir/spark/bin
	echo "bin dir of Spark: " $sparkBinDir
	
	# invoke Spark with DA as parameter	
	$sparkBinDir/spark-submit --master spark://127.0.1.1:7077 $home_da_dir/$da_file_name
	
	fi
done
