Welcome
=======
Welcome to the Terracotta Server Samples directory.

This directory contains a couple of examples that show you how to use various
features and configurations of Terracotta Server. 
The latest version of the code samples can be found in Subversion at
http://svn.terracotta.org/svn/tc/dso/trunk/terracotta-samples/

How to run the Terracotta Samples 
==========================

1. Launch the server
Launch the server with either the start-sample-server.bat script (for Windows) or the start-sample-server.sh script (for Linux/UNIX) found in each sample repository.

Note: After running the samples, stop the server using either the stop-sample-server.bat script (for Windows) or the stop-sample-server.sh (for Linux/UNIX) found in each sample repository.

2. Configure and run the samples using either the scripts or Maven procedure below.

Using a script:

A. Configuration
Be sure to have the JAVA_HOME defined.
For Windows users, you can use the following command:
set JAVA_HOME=path/to/java/home
For UNIX/Linux users, you can use the following command:
export JAVA_HOME=path/to/java/home

B. Launching the sample
Each example is located in its own subdirectory.
Every example can be run using Windows .bat files or Linux/UNIX .sh files.
Additionally, each example directory contains a README.txt file. Please
read this file first, as it will contain useful information for running
the samples.

Running the samples with Maven:

All samples can be run via Maven commands. After starting the server:

%> mvn install ( run at least once to compile the samples )
%> mvn exec:exec -P mySample (where mySample is the name of the test (crud, search, configprogrammatic ...). See details in next paragraphs.

Basic configuration samples:
configprogrammatic - Configure BigMemory programatically
configfile - Configure BigMemory via xml
arc - Configure BigMemory with Automatic Resource Control

Samples demonstrating features:
crud - Basic BigMemory operations - create, retrieve, update, delete
search - BigMemory Search features
cache - Use BigMemory as a cache
nonstop - BigMemory nonstop and rejoin features
wan - BigMemory WAN replication
