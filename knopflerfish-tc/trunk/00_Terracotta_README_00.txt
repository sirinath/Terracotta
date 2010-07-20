These are the sources used to produce the knopflerfish-tc jar we use
in core terracotta

To produce a new jar, cd to "osgi/framework" and run "ant clean jar"

That should produce a new framework.jar in the parent directory

That framework.jar should be uploaded to nexus putting the build date
in the maven version. For example the build done on July 20, 2010 the
jar is named:

  knopflerfish-tc-2.0.1-20100720.jar

Once in nexus you can update all the references in core (ivy.xml and
the top level pom.xml)

