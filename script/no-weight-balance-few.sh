#!/bin/bash

javac DirectoryServiceImpl.java DaemonServiceImpl.java DownloadServiceImpl.java

java DirectoryServiceImpl config/directory-no-balance.properties &
sleep 2

java DaemonServiceImpl config/directory-no-balance.properties config/daemon-balance1.properties &> log/daemon-no-few-many1.log &
sleep 2

java DaemonServiceImpl config/directory-no-balance.properties config/daemon-balance2.properties &> log/daemon-no-few-many2.log &
sleep 2

java DaemonServiceImpl config/directory-no-balance.properties config/daemon-balance3.properties &> log/daemon-no-few-many3.log &
sleep 2


for i in $(seq 3 $n); do
    java DownloadServiceImpl config/directory-no-balance.properties random.data 1 para  &> "log/no-balance-few-download1-$i.log" &
    sleep 1
done

for i in $(seq 2 $n); do
    java DownloadServiceImpl config/directory-no-balance.properties random2.data 1 para  &> "log/no-balance-few-download2-$i.log" &
    sleep 1
done

start=$(date +%s.%N)
java DownloadServiceImpl config/directory-no-balance.properties repeating.data 3 para 
end=$(date +%s.%N)

runtime=$(echo "$end - $start" | bc)

echo "Download runtime: $runtime seconds"

pkill -f "java"
