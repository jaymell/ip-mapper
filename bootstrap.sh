#!/bin/bash

export MONGO_URI="mongodb://$(/sbin/ip route|awk '/default/ { print $3 }'):27017/logger"

java -jar app.jar

