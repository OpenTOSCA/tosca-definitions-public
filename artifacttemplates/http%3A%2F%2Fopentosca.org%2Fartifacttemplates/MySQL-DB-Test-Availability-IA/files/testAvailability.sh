#!/bin/sh
if mysql -u"${DBUser}" -p"${DBPassword}" -h"localhost" -e "use ${DBName}" >/dev/null 2>&1; then
    echo "Result=success"
else
    echo "Result=failure"
fi
