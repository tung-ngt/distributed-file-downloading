### All script for the test case of the services are in the script folder

### Running script

'''
cd <base_project_folder>
script/<script>.sh
'''


### Generate test data

'''
script/generate-data.sh
'''


### Kill all service

'''
script/killall.sh
'''


### Manual compiling

'''
javac DirectoryServiceImpl.java DaemonServiceImpl.java DownloadServiceImpl.java Client.java
'''


### Config files

See the config files in config/ for examples


### Running directory manualy

'''
java DirectoryServiceImpl <path-to-dir-config-file>
'''


### Running daemon manualy

'''
java DaemonServiceImpl <path-to-dir-config-file> <path-to-daemon-config-file>
'''


### Running download manually

'''
java DownloadServiceImpl <path-to-dir-config-file> <file-to-download> <number-of-favorable-sources> <path-to-download-folder> <para|seq>
'''

number-of-favorable-sources is the number of maximum source to use for downloading (directory may return smaller number of sources)


### Running the client implement both daemon and download

'''
java Client <path-to-dir-config-file> <path-to-daemon-config-file>
'''
