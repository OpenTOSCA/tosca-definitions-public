#!/bin/bash
echo "### Install PHP WebApplication" >> ~/management.log 2>&1;
sudo apt-get -qq update;
sudo apt-get -qqy install unzip;

#find csar root
csarRoot=$(find ~ -maxdepth 1 -path "*.csar");

IFS=';' read -ra NAMES <<< "$DAs";
for i in "${NAMES[@]}"; do
        #echo "KeyValue-Pair: "
        #echo $i
        IFS=',' read -ra entry <<< "$i";    
        #echo "Key: "
        #echo ${entry[0]}
        #echo "Value: "
        #echo ${entry[1]}
        if [[ "${entry[1]}" == *.zip ]];
        then    
		applicationName=$(basename ${entry[1]} .zip)
		echo $applicationName
		mkdir /var/www/html/${entry[0]}
        sudo unzip $csarRoot${entry[1]} -d /var/www/html/${entry[0]}
	fi
done


#sudo cp ~/ApacheTest.csar/artifacttemplates/http%253A%252F%252Fopentosca.org%252Fartifacttemplates%252F/PHP-5-WebApplication-ArtifactTemplate-Application/files/WebApplication.zip /var/www/;
#cd /var/www/;
#sudo unzip /var/www/WebApplication.zip;
#sudo mv /var/www/index.php /var/www/html/index.php;
