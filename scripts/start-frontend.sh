#!/bin/bash
# Start the frontend dev server for local review.
cd /home/nunez/scholastic-assessment/frontend
nohup npm run dev > /tmp/vite.log 2>&1 &
echo $! > /tmp/vite.pid
sleep 5
curl -s -o /dev/null -w "frontend ready at http://localhost:5173 : %{http_code}\n" http://localhost:5173/login
