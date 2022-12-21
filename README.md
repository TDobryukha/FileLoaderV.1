# FileLoaderV.1
Downloads files via HTTP in several streams with a limitation on the download speed. Implementation option No. 1. 
Semaphore is used to limit the number of threads, CountDownLatch is used to track the download of all threads, and RateLimiter is used to limit the speed.
The user needs to specify:
1. the full path to the text file, which contains a list of links of what needs to be downloaded. (One link per line)
2. Full path to the folder where you want to upload files
3. Number of threads for downloading files simultaneously
4. Limit on download speed (in KB)
