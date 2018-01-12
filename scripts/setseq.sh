user=$1
password=$2
projectid=$3
newseq=$4
curl -v -X POST -H "Content-Type:application/json" http://localhost:8080/nci_protege/login -d "{\"user\":\"$user\", \"password\":\"$password\"}" | ./jq --raw-output '. | .userid, .token' > usertok

res=""
for i in `cat usertok`
do
    res="${res}${i}"
    res="${res}:"
done

AUTH=`echo -n ${res%?} | openssl enc -base64 | tr -d "\n"`
echo $AUTH

curl -v -X POST -H "Authorization: Basic ${AUTH}" "http://localhost:8080/nci_protege/server/setcodegenseq?projectid=${projectid}&seq=${newseq}"