def repoDir = new File(command.geronimoHome, 'repository')
def installRoot = new File(command.geronimoHome, 'var/terracotta')
def sessionJar = new File(repoDir, 'org/terracotta/tc-session/${terracottaVersion}/tc-session-${terracottaVersion}.jar')
def bootJar = new File(installRoot, 'lib/dso-boot/dso-boot.jar')
if (!bootJar.exists()){
    def ant = new AntBuilder()
    def tcConfig = new File(installRoot, 'tc-config-geronimo.xml')
    ant.path(id: "cp"){
        pathelement(location: new File(repoDir, 'org/terracotta/terracotta/${terracottaVersion}/terracotta-${terracottaVersion}.jar'))
        pathelement(location: new File(repoDir, 'org/terracotta/tcconfig1/${tcConfig1Version}/tcconfig1-${tcConfig1Version}.jar'))
        pathelement(location: new File(repoDir, 'org/terracotta/tcconfig2/${tcConfig2Version}/tcconfig2-${tcConfig2Version}.jar'))
        pathelement(location: new File(repoDir, 'org/terracotta/tcconfig/${terracottaVersion}/tcconfig-${terracottaVersion}.jar'))
        pathelement(location: new File(repoDir, 'org/mortbay/jetty/jetty/${jettyVersion}/jetty-${jettyVersion}.jar'))
        pathelement(location: new File(repoDir, 'org/mortbay/jetty/jsp-2.1/${jettyVersion}/jsp-2.1-${jettyVersion}.jar'))
        pathelement(location: new File(repoDir, 'org/mortbay/jetty/jetty-util/${jettyVersion}/jetty-util-${jettyVersion}.jar'))
        pathelement(location: new File(repoDir, 'javax/servlet/servlet-api/${servletApiVersion}/servlet-api-${servletApiVersion}.jar'))
        pathelement(location: new File(repoDir, 'javax/management/jmxri/${jmxRiVersion}/jmxri-${jmxRiVersion}.jar'))
        pathelement(location: new File(repoDir, 'javax/management/jmxremote/${jmxRemoteVersion}/jmxremote-${jmxRemoteVersion}.jar'))
        pathelement(location: new File(repoDir, 'javax/management/jmxremote_optional/${jmxRemoteVersion}/jmxremote_optional-${jmxRemoteVersion}.jar'))
        pathelement(location: new File(repoDir, 'org/beanshell/bsh/${beanshellVersion}/bsh-${beanshellVersion}.jar'))
        pathelement(location: new File(repoDir, 'berkleydb/je/${berkleyDbVersion}/je-${berkleyDbVersion}.jar'))
        pathelement(location: new File(repoDir, 'commons-cli/commons-cli/${commonsCliVersion}/commons-cli-${commonsCliVersion}.jar'))
        pathelement(location: new File(repoDir, 'commons-io/commons-io/${commonsIoVersion}/commons-io-${commonsIoVersion}.jar'))
        pathelement(location: new File(repoDir, 'commons-collections/commons-collections/${commonsCollectionsVersion}/commons-collections-${commonsCollectionsVersion}.jar'))
        pathelement(location: new File(repoDir, 'commons-lang/commons-lang/${commonsLangVersion}/commons-lang-${commonsLangVersion}.jar'))
        pathelement(location: new File(repoDir, 'xmlbeans/xbean/${xmlbeansVersion}/xbean-${xmlbeansVersion}.jar'))
        pathelement(location: new File(repoDir, 'stax/stax-api/${staxVersion}/stax-api-${staxVersion}.jar'))
        pathelement(location: new File(repoDir, 'log4j/log4j/${log4jVersion}/log4j-${log4jVersion}.jar'))
        pathelement(location: new File(repoDir, 'concurrent/concurrent/${concurrentVersion}/concurrent-${concurrentVersion}.jar'))
        pathelement(location: new File(repoDir, 'trove/trove/${troveVersion}/trove-${troveVersion}.jar'))
        pathelement(location: new File(repoDir, 'knopflerfish-tc/knopflerfish-tc/${knopflerfishVersion}/knopflerfish-tc-${knopflerfishVersion}.jar'))
    }
    ant.property(name:'tcp', refid: "cp")
    def tcPath=ant.antProject.getProperty('tcp')

    ant.java(classname: 'com.tc.object.tools.BootJarTool', classpathref:"cp", fork:true) {
        arg(value: '-o')
        arg(value: bootJar)
        arg(value: '-f')
        arg(value: tcConfig)
        arg(value: 'make')

        sysproperty(key:"geronimo-terracotta.home", value:tcConfig)
        sysproperty(key:"tc.classpath", value:tcPath)
        sysproperty(key:"tc.install-root", value:installRoot)
        sysproperty(key:"tc.session.classpath", value:sessionJar)
        sysproperty(key:"com.tc.l1.modules.repositories", value:repoDir.toURL())
    }   
}   

command.javaFlags << "-Xbootclasspath/p:${bootJar} -XX:MaxPermSize=256m"
