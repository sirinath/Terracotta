Terracotta
==========

Documentation: http://www.terracotta.org/web/display/orgsite/Documentation 
Forums:        http://forums.terracotta.org
Mailing Lists: http://lists.terracotta.org
Services:      http://www.terracotta.org/web/display/enterprise/Support


Getting Started 
--------------- 
If you are new to Terracotta, start with the Terracotta Welcome application. 

Terracotta 
   Unix/Linux: 
      - Unzip/untar the Terracotta software. 
      - cd into the top-level directory. 
      - Launch the Welcome application by entering the command: 
           ./welcome.sh 
   Windows: 
      - Run the Terracotta installer. 
      - Launch the Welcome application by entering the command: 
           welcome.bat 
      - Alternatively, you can launch the Welcome application from the start menu: 
           Programs --> Terracotta --> Terracotta Welcome 

Terracotta DSO Eclipse Plug-in 
   Follow the installation instructions at: 
      - http://www.terracotta.org/web/display/docs/Eclipse+Plugin+Guide 


Sign Up 
-------------- 
To get the most out of your Terracotta experience, sign up for a Terracotta account: 
    - http://www.terracotta.org/web/display/orgsite/Sign-up 

With a Terracotta account, you can: 
    * Access all services with a site-wide ID 
    * Post questions to the Forums 
    * Submit JIRA reports 
    * Vote on bugs to be fixed, or features for the next release 
     
     
Configuration File Samples 
-------------------------- 
The tc-config-reference.xml file, found in the Terracotta docs directory, contains 
example settings appropriate for a typical deployment. This file is a reference 
containing definitions of the available configuration options. 


Sample Applications 
------------------- 
The Terracotta samples directory contains sample applications that illustrate the use 
of Terracotta in clustering the JVM. The sample applications for Terracotta Sessions 
are available from links in Configurator. For Terracotta for Spring, the sample 
applications are in the "samples/spring" directory. For Terracotta Pojos, the 
sample applications are in the "samples/pojo" directory. 


Administration Tools 
-------------------- 
The "bin" directory contains a script to launch the Terracotta Developer Console. 

Unix/Linx: 
   ./dev-console.sh 

Windows: 
   dev-console.bat 

The Developer Console provides an inside view of cluster activity at run-time. 


Terracotta Forge 
-------------------- 
Terracotta integration modules (TIMs) enable integration with many common frameworks 
such as EHCache and Quartz. For the latest information visit: 

    - http://www.terracotta.org/web/display/orgsite/Integration+Guides 

Projects for the Forge can be found at: 
    - http://forge.terracotta.org 

      
Java Virtual Machine 
-------------------- 
Terracotta clusters can be run with any supported JRE. The product guide 
contains a list of supported JREs. The JRE used by Terracotta Clients need not 
match the one used by the Terracotta Server. You can designate the JVM that Terracotta 
uses by setting the JAVA_HOME environment variable. 

---- 
Copyright (c) 2009, Terracotta, Inc. 
http://www.terracotta.org



