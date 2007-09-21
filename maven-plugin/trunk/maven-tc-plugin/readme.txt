
  This is a Maven plugin for Terracotta


  This version is not avaialable from any snapshot repositories yet, but you can install it locally

  Additional artifacts required (all available from Terracotta install):

set tc=C:\dev\terr\tc-trunk\code\base\build\dist\terracotta-trunk\lib
  
call mvn install:install-file -DgeneratePom=true -DgroupId=org.terracotta -DartifactId=terracotta -Dversion=2.4 -Dpackaging=jar -Dfile=%tc%\tc.jar
call mvn install:install-file -DgeneratePom=true -DgroupId=org.terracotta -DartifactId=tcconfig -Dversion=2.4 -Dpackaging=jar -Dfile=%tc%\tcconfig-xmlbeans-generated.jar
call mvn install:install-file -DgeneratePom=true -DgroupId=org.terracotta -DartifactId=tcconfig1 -Dversion=1.0 -Dpackaging=jar -Dfile=%tc%\tcconfigV1.jar
call mvn install:install-file -DgeneratePom=true -DgroupId=org.terracotta -DartifactId=tcconfig2 -Dversion=2.0 -Dpackaging=jar -Dfile=%tc%\tcconfigV2.jar

rem http://java.sun.com/products/JavaManagement/download.html
call mvn install:install-file -DgroupId=javax.management -DartifactId=jmxri -Dversion=1.2.1 -Dpackaging=jar -Dfile=%tc%\jmxri-1.2.1.jar
call mvn install:install-file -DgroupId=com.sun.jdmk -DartifactId=jmxtools -Dversion=1.2.1 -Dpackaging=jar -Dfile=%tc%\jmxtools-1.2_8.jar
call mvn install:install-file -DgroupId=javax.management -DartifactId=jmxremote -Dversion=1.0.1_04 -Dpackaging=jar -Dfile=%tc%\jmxremote-1.0.1_04.jar
call mvn install:install-file -DgroupId=javax.management -DartifactId=jmxremote_optional -Dversion=1.0.1_04 -Dpackaging=jar -Dfile=%tc%\jmxremote_optional-1.0.1_04-b58.jar


  Ideally these dependencies should be deployed to our own
  Maven repository, i.e. http://download.terracotta.org/maven2/

  For more information see http://www.terracotta.org/confluence/display/wiki/Terracotta+Maven+Plugin

