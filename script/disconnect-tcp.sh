#!/bin/bash
javac DirectoryServiceImpl.java DaemonServiceImpl.java DownloadServiceImpl.java


script/killall.sh
echo "starting directory"
java DirectoryServiceImpl config/directory.properties &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon1.properties &> log/daemon-not-disconnect1.log &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon2.properties &> log/daemon-not-disconnect2.log &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon-disconnect-tcp1.properties &> log/daemon-disconnect-tcp1.log &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon-disconnect-tcp2.properties &> log/daemon-disconnect-tcp2.log &
sleep 2

start=$(date +%s.%N)
java DownloadServiceImpl config/directory.properties repeating.data download 4 para
end=$(date +%s.%N)

runtime=$(echo "$end - $start" | bc)
echo "Download runtime: $runtime seconds"

script/killall.sh
