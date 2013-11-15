
A. SETTING JAVA_HOME
----------------------------------------------------
To start the services, you have to set your JAVA_HOME in conf/wrapper-tsa.conf, ie.

set.JAVA_HOME=C:/Java/jdk1.7.0_21

NOTE: Wrapper doesn't read your JAVA_HOME from environment. For Windows, if you don't want to set it in the configuration file,
please comment it out and set JAVA_HOME in the registry instead.



B. CONFIGURATION FILES
----------------------------------------------------
1. For TSA, you will need 1 configuration file

conf/tc-config.xml

Please overwrite this file with your own. If you want to change the file name, you can modify the
name in wrapper configuration.

2. The TSA conf/wrapper-tsa.conf will need to know which server you want to start. So modify this line:

set.SERVER_NAME=server0 

to the one in your tc-config.xml


C. PERMISSION
----------------------------------------------------
The services will be controlled by an Administrators user so you have to confirm for every action 
(install, start, stop, remove, etc). 

Incidentally, the "wrapper" folder will need to have read/write permission for Administrators user.


D. RUNNING SERVICES
----------------------------------------------------
The service needs to be installed once before it can be started. To install:

%> bin/tsa-service.bat install

Then you can either start/stop the service:

%> bin/tsa-service.bat start
%> bin/tsa-service.bat stop

If you want to remove the service:

%> bin/tsa-service.bat remove

There are more commands available if you just run the script without any parameter:

%> bin/tsa-service.bat


E. CHANGING WRAPPER CONFIGURATIONS
----------------------------------------------------

There are comments in wrapper-tsa.conf to explain each parameter.
If you need to modify JVM system properties, classpath, command line parameters, etc, 
follow the current pattern. Please pay close attention to their numerical
order and param count. More details at:

http://wrapper.tanukisoftware.com/doc/english/properties.html


