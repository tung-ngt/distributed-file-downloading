#!/bin/bash

pkill -f "java DownloadServiceImpl"
pkill -f "java DaemonServiceImpl"
pkill -f "java DirectoryServiceImpl"
echo "killall running service"
sleep 1
