#!/bin/bash

# Docker 설치
apt-get update
apt-get install -y docker.io
systemctl start docker
systemctl enable docker

# Docker 네트워크 생성
docker network create krew-network

# MySQL 컨테이너 실행
docker run -d \
  --name mysql-container \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=${mysql_password} \
  --network krew-network \
  mysql:latest


# Spring Boot 백엔드 실행
docker run -d \
  --name vitaltrip \
  -p 8080:8080 \
  --env-file .env \
  --network krew-network \
  adorableco/vitaltrip:1.0
