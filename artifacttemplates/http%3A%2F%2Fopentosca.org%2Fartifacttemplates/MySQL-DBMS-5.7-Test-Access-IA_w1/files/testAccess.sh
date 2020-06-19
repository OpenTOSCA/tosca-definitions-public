#!/bin/sh
if sudo mysql -u"${DBMSUser}" -p"${DBMSPassword}" -h"localhost" -e "\help" >/dev/null 2>&1; then
    echo "Result=success"
else
    echo "Result=failure"
fi

