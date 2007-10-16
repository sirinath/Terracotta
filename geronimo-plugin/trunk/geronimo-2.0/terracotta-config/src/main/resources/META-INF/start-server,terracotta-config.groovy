def installRoot = new File(command.geronimoHome, 'var/terracotta')
def repoDir = new File(command.geronimoHome, 'repository')
def sessionJar = new File(repoDir, 'org/terracotta/tc-session/${terracottaVersion}/tc-session-${terracottaVersion}.jar')

def ant = new AntBuilder()
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

command.properties['tc.install-root'] = "${installRoot}"
command.properties['tc.config'] = "${installRoot}/tc-config-geronimo.xml"
command.properties['tc.classpath'] = tcPath
command.properties['com.tc.l1.modules.repositories'] = repoDir.toURL()
command.properties['geronimo-terracotta.home'] = installRoot
command.properties['tc.session.classpath'] = sessionJar
