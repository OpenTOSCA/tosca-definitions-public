#!/bin/sh
rootUser=$DBMSUser;
rootPw=$DBMSPassword;
port=$DBMSPort;

echo "Received $rootUser as user, $rootPw as password, $port as port";

mySqlConfig='/etc/mysql/my.cnf'

# remove bind so that it can be accessed later
sed -i '/^bind-address/ d' $mySqlConfig 

#set root user and pw (we assume here that the install.sh was executed before and the root user is still available, in a management plan this script might fail hard)
mysql -uroot -e "use mysql; update user set user='$rootUser' where user='root'; update user set password=PASSWORD('$DBMSPassword') where user='$rootUser'; grant all privileges on *.* to '$rootUser'@'%'; flush privileges;"

#set port
sed -i -e "/port	/c\port=$DBMSPort" $mySqlConfig;
#configure iptables
iptables -A INPUT -p tcp -m tcp --dport $DBMSPort -j ACCEPT

service mysql restart;
