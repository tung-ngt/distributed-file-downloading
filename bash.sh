#!/bin/bash

n=10  # Number of runs
total=0

for i in $(seq 1 $n); do
	echo "$i"
    start=$(date +%s.%N)
    java DownloadServiceImpl directory.properties latest.pth 2 seq
    end=$(date +%s.%N)
    runtime=$(echo "$end - $start" | bc)
    total=$(echo "$total + $runtime" | bc)
done

average=$(echo "scale=6; $total / $n" | bc)
echo "Average execution time: $average seconds"
