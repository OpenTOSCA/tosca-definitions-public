#!/bin/bash
databaseuser=$DBUser
databasepw=$DBPassword
databasename=$DBName

#find csar root
csarRoot=$(find ~ -maxdepth 1 -path "*.csar");

# iterate over map of DAs
IFS=';' read -ra NAMES <<< "$DAs";
for i in "${NAMES[@]}"; do
	echo "KeyValue-Pair: "
    echo $i
    IFS=',' read -ra PATH <<< "$i";
    	echo "Key: "
    	echo ${PATH[0]}
    	echo "Value: "
    	echo ${PATH[1]}

	# is it an sql file ? at least the ending
	if [[ "${PATH[1]}" == *.sql ]];
	then
		# connect to database and dump in the statements
		/usr/bin/mysql -u$databaseuser -p$databasepw -D$databasename < $csarRoot${PATH[1]};
	fi
done
