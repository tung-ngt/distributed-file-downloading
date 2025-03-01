#!/bin/bash

javac DirectoryServiceImpl.java DaemonServiceImpl.java DownloadServiceImpl.java

echo "compressing"
start1=$(date +%s.%N)
tar -czf data/repeating.data.tar.gz data/repeating.data
end1=$(date +%s.%N)
runtime1=$(echo "$end1 - $start1" | bc)
echo "compress runtime: $runtime1 seconds"

java DirectoryServiceImpl config/directory.properties &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon-compress1.properties &> log/daemon-compress1.log &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon-compress2.properties &> log/daemon-compress2.log &
sleep 2


start2=$(date +%s.%N)
java DownloadServiceImpl config/directory.properties repeating.data.tar.gz download 2 para 
end2=$(date +%s.%N)
runtime2=$(echo "$end2 - $start2" | bc)
echo "Download runtime: $runtime2 seconds"

start3=$(date +%s.%N)
tar -xzf download/repeating.data.tar.gz
end3=$(date +%s.%N)
runtime3=$(echo "$end3 - $start3" | bc)
echo "decompressing runtime: $runtime3 seconds"

runtime=$(echo "$runtime2 + $runtime3" | bc)
echo "Download & decompress runtime: $runtime seconds"

pkill -f "java"
