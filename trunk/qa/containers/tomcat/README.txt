The ant build script can be used to install and run a sample tomcat application.
Run "ant -p" to get usage information.

executing:

% ant

should download and install tomcat, and build and deploy a simple servlet.

To run a second version on a different port, execute:

% ant -Dtomcat.port=7777

Tomcat is run in the background, using the tomcat start script.  To shut it down or restart it, do:

% ant start.tomcat
or
% ant stop.tomcat


There is also a "run.tomcat" task which launchs ant from a java command, rather than going through a start 
script.  Using this task, the tomcat process will die when ant exits.

