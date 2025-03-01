#!/bin/bash

javac DirectoryServiceImpl.java DaemonServiceImpl.java DownloadServiceImpl.java

java DirectoryServiceImpl directory.properties &
sleep 2

java DaemonServiceImpl directory.properties daemon-client1.properties &> daemon-client1.log &
sleep 2

java DaemonServiceImpl directory.properties daemon-client2.properties &> daemon-client2.log &
sleep 2

java DaemonServiceImpl directory.properties daemon-client3.properties &> daemon-client3.log &
sleep 2


for i in $(seq 3 $n); do
    java DownloadServiceImpl directory.properties client1.pth 1 para  &> "download-client1-$i.log" &
done

for i in $(seq 2 $n); do
    java DownloadServiceImpl directory.properties client2.pth 1 para  &> "download-client2-$i.log" &
done

sleep 3
start=$(date +%s.%N)
java DownloadServiceImpl directory.properties latest.pth 3 para 
end=$(date +%s.%N)

runtime=$(echo "$end - $start" | bc)

echo "Download runtime: $runtime seconds"

pkill -f "java"
