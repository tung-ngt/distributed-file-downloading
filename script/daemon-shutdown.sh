#!/bin/bash
javac DirectoryServiceImpl.java DaemonServiceImpl.java DownloadServiceImpl.java

script/killall.sh
script/killall.sh
echo "starting directory"

java DirectoryServiceImpl config/directory.properties &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon1.properties &> log/daemon-not-shudown1.log &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon2.properties &> log/daemon-not-shutdown2.log &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon3.properties &> log/daemon-shudown1.log &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon4.properties &> log/daemon-shudown2.log &
sleep 2

java DownloadServiceImpl config/directory.properties repeating.data download 4 para &
sleep 1
pkill -f "java DaemonServiceImpl config/directory.properties config/daemon3.properties"
pkill -f "java DaemonServiceImpl config/directory.properties config/daemon4.properties"

sleep 5
script/killall.sh
