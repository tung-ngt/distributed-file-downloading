#!/bin/bash

javac DirectoryServiceImpl.java DaemonServiceImpl.java DownloadServiceImpl.java

echo "compressing"
start1=$(date +%s.%N)
tar -czf data/random.data.tar.gz data/random.data
end1=$(date +%s.%N)
runtime1=$(echo "$end1 - $start1" | bc)
echo "compress runtime: $runtime1 seconds"

script/killall.sh
echo "starting directory"

java DirectoryServiceImpl config/directory.properties &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon-compress-random1.properties &> log/daemon-compress-random1.log &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon-compress-random2.properties &> log/daemon-compress-random2.log &
sleep 2


start2=$(date +%s.%N)
java DownloadServiceImpl config/directory.properties random.data.tar.gz download 2 para 
end2=$(date +%s.%N)
runtime2=$(echo "$end2 - $start2" | bc)
echo "Download runtime: $runtime2 seconds"

start3=$(date +%s.%N)
tar -xzf download/random.data.tar.gz
end3=$(date +%s.%N)
runtime3=$(echo "$end3 - $start3" | bc)
echo "decompressing runtime: $runtime3 seconds"

runtime=$(echo "$runtime2 + $runtime3" | bc)
echo "Download & decompress runtime: $runtime seconds"

script/killall.sh
