Terracotta 3.2.1 - Simple Scale for Enterprise Java
===================================================

Terracotta offers powerful solutions for scaling Java applications and 
accelerating their performance. This kit includes the full suite of Terracotta 
Product capabilities, including Ehcache and Ehcache for Hibernate (/ehcache), 
Quartz including the Terracotta Job Store for Quartz (/quartz) and Terracotta 
for Web Sessions (/sessions).

This document helps you find the information needed to implement these 
solutions for your use case.


Getting Started
---------------

For more information on getting started with Terracotta, begin with the 
Terracotta website at http://www.terracotta.org/kit/reflector?kitID=3.2.1&pageID=GettingStarted

To locate Terracotta documentation, see 
http://www.terracotta.org/kit/reflector?kitID=3.2.1&pageID=Documentation


Join the Terracotta Community
-----------------------------
Enjoy the following benefits by signing up for a Terracotta account:

   -> Access all services with a site-wide ID
   -> Post questions to the Forums
   -> Submit JIRA reports
   -> Vote on bugs to be fixed, or features for the next release 

Sign up at http://www.terracotta.org/kit/reflector?kitID=3.2.1&pageID=Register


Resources
---------
The latest release notes are available on the web at 
http://www.terracotta.org/kit/reflector?kitID=3.2.1&pageID=ReleaseNotes

The following directories under the Terracotta home directory contain useful 
examples and information:


- ehcache: libraries and config files for distributed caching and hibernate 
   second-level cache

- quartz: libraries, config files and samples for clustered job scheduling

- sessions: libraries, config files and samples for web session clustering

- bin: scripts for starting, stopping, administering, monitoring, and 
   maintaining a Terracotta cluster
 
- docs: contains a reference configuration file for express terracotta,
   tc-config-express-reference.xml 
   
- platform: contains config examples, tools, modules and samples for custom/DSO usage

 
Help and additional information is available on the web at the following URLs:

 - Product Documentation: 
    http://www.terracotta.org/kit/reflector?kitID=3.2.1&pageID=ProductDocumentation 
 - Forums:        http://forums.terracotta.org
 - Mailing Lists: http://lists.terracotta.org
 - Services:      
    http://www.terracotta.org/kit/reflector?kitID=3.2.1&pageID=SupportServices


Java Virtual Machine
--------------------

The platform support page in the product documentation contains a list of supported JREs.  
The JRE used by Terracotta Clients need not match the one used by the Terracotta Server. 
Control the JVM that the Terracotta uses by setting the JAVA_HOME environment variable.

----
All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved. 
http://www.terracotta.org