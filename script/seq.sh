#!/bin/bash

javac DirectoryServiceImpl.java DaemonServiceImpl.java DownloadServiceImpl.java

script/killall.sh
echo "starting directory"
java DirectoryServiceImpl config/directory.properties &
sleep 2

sources=10
for i in $(seq 1 $sources); do
    java DaemonServiceImpl config/directory.properties "config/daemon$i.properties" &> "log/daemon-seq-$i.log" &
    sleep 1
done

for i in $(seq 1 $sources); do
  echo "$i sources"
  total=0
  n=10
  for j in $(seq 1 $n); do
      echo "run $j"
      start=$(date +%s.%N)
      java DownloadServiceImpl config/directory.properties repeating.data download "$i" seq &> "log/download-seq-$i.log"
      end=$(date +%s.%N)
      runtime=$(echo "$end - $start" | bc)
      total=$(echo "$total + $runtime" | bc)
  done
  average=$(echo "scale=6; $total / $n" | bc)
  echo "Average execution time $i sources: $average seconds"
done

script/killall.sh
