#! /bin/bash
command -v lsof >/dev/null 2>&1 || { sleep 5; return 1; }
sudo lsof -n -iTCP:${Port} | grep LISTEN >/dev/null 2>&1 || { sleep 5; echo "FAILED"; return 1; }
sleep 5
echo "SUCCESS"
return 0
