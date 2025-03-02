#!/bin/bash
javac DirectoryServiceImpl.java DaemonServiceImpl.java DownloadServiceImpl.java


script/killall.sh
echo "starting directory"
java DirectoryServiceImpl config/directory.properties &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon1.properties &> log/daemon-not-not-fail-checksum.log &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon-fail-checksum1.properties &> log/daemon-fail-checksum1.log &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon-fail-checksum2.properties &> log/daemon-fail-checksum2.log &
sleep 2


start=$(date +%s.%N)
java DownloadServiceImpl config/directory.properties repeating.data download 3 para
end=$(date +%s.%N)

runtime=$(echo "$end - $start" | bc)
echo "Download runtime: $runtime seconds"

script/killall.sh
