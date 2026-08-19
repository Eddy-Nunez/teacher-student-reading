#!/bin/bash
# Start/restart the portal backend. Kill strategy uses /proc inspection (safe against
# accidentally matching this script's own cmdline).
cd /home/nunez/scholastic-assessment/backend
for p in $(pgrep -x java); do
  cmd=$(tr '\0' ' ' < /proc/$p/cmdline 2>/dev/null)
  case "$cmd" in
    *scholastic*portal*|*target*SNAPSHOT*) kill "$p" 2>/dev/null ;;
  esac
done
sleep 2
rm -rf data
nohup java -jar target/portal-0.0.1-SNAPSHOT.jar --server.port=8080 > /tmp/portal.log 2>&1 &
echo $! > /tmp/portal.pid
sleep 13
echo "started pid $(cat /tmp/portal.pid)"
curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"teacher","password":"password"}' | head -c 40
echo
