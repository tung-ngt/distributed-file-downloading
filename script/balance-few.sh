#!/bin/bash

javac DirectoryServiceImpl.java DaemonServiceImpl.java DownloadServiceImpl.java

script/killall.sh
echo "starting directory"

java DirectoryServiceImpl config/directory.properties &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon-balance1.properties &> log/daemon-balance-few1.log &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon-balance2.properties &> log/daemon-balance-few2.log &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon-balance3.properties &> log/daemon-balance-few3.log &
sleep 2


for i in $(seq 3 $n); do
    java DownloadServiceImpl config/directory.properties random.data 1 download para  &> "log/balance-few-download1-$i.log" &
    sleep 1
done

for i in $(seq 2 $n); do
    java DownloadServiceImpl config/directory.properties random2.data 1 download para  &> "log/balance-few-download2-$i.log" &
    sleep 1
done

start=$(date +%s.%N)
java DownloadServiceImpl config/directory.properties repeating.data 3 download para 
end=$(date +%s.%N)

runtime=$(echo "$end - $start" | bc)

echo "Download runtime: $runtime seconds"

script/killall.sh
