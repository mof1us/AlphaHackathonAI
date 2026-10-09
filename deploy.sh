# bash

docker compose down
git pull origin master
docker compose up -d --build

echo "Wait 60 seconds while containers up"
sleep 60

echo "Statuses of containers: "

docker compose ps
