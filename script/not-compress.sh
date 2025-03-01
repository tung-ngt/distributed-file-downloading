#!/bin/bash
javac DirectoryServiceImpl.java DaemonServiceImpl.java DownloadServiceImpl.java


java DirectoryServiceImpl config/directory.properties &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon-not-compress1.properties &> log/daemon-not-compress1.log &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon-not-compress2.properties &> log/daemon-not-compress2.log &
sleep 2


start=$(date +%s.%N)
java DownloadServiceImpl config/directory.properties repeating.data 2 para 
end=$(date +%s.%N)

runtime=$(echo "$end - $start" | bc)
echo "Download runtime: $runtime seconds"

pkill -f "java"
