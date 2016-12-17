#!/bin/sh
port=$1
user=$2
password=$3

# if troubleshooting, add -v to the two curl commands
curl -X POST -H "Content-Type:application/json" http://localhost:$port/nci_protege/login -d "{\"user\":\"$user\", \"password\":\"$password\"}" | jq --raw-output '. | .userid, .token' > usertok

res=""
for i in `cat usertok`
do
    res="${res}${i}"
    res="${res}:"
done
AUTH=`echo -n ${res%?} | openssl enc -base64 | tr -d "\n"`

#if troubleshooting, uncomment the following line
#echo $AUTH

curl -X POST -H "Authorization: Basic ${AUTH}" http://localhost:$port/nci_protege/server/shutdown | jq '.'
