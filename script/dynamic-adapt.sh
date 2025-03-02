#!/bin/bash
javac DirectoryServiceImpl.java DaemonServiceImpl.java DownloadServiceImpl.java

script/killall.sh
echo "starting directory"

java DirectoryServiceImpl config/directory.properties &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon1.properties &> log/dynamic-adapt1.log &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon2.properties &> log/dynamic-adapt2.log &
sleep 2

java DownloadServiceImpl config/directory.properties repeating.data download 4 para 
java DownloadServiceImpl config/directory.properties random.data download 4 para 

pkill -f "java DaemonServiceImpl config/directory.properties config/daemon2.properties"
sleep 2

java DownloadServiceImpl config/directory.properties repeating.data download 4 para 
java DownloadServiceImpl config/directory.properties random.data download 4 para 


java DaemonServiceImpl config/directory.properties config/daemon2.properties &> log/dynamic-adapt3.log &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon3.properties &> log/dynamic-adapt4.log &
sleep 2

java DaemonServiceImpl config/directory.properties config/daemon-adapt.properties &> log/dynamic-adapt5.log &
sleep 2

java DownloadServiceImpl config/directory.properties repeating.data download 4 para 
java DownloadServiceImpl config/directory.properties random.data download 4 para 

sleep 2
script/killall.sh
