def installRoot = new File(command.geronimoHome, 'var/terracotta')
def repoDir = new File(command.geronimoHome, 'repository')
def ant = new AntBuilder()
ant.path(id: "cp"){
            pathelement(location: new File(repoDir, 'org/terracotta/terracotta/2.4/terracotta-2.4.jar'))
            pathelement(location: new File(repoDir, 'org/terracotta/tcconfig1/1.0/tcconfig1-1.0.jar'))
            pathelement(location: new File(repoDir, 'org/terracotta/tcconfig2/2.0/tcconfig2-2.0.jar'))
            pathelement(location: new File(repoDir, 'org/terracotta/tcconfig/2.4/tcconfig-2.4.jar'))
            pathelement(location: new File(repoDir, 'org/mortbay/jetty/jetty/6.0.1/jetty-6.0.1.jar'))
            pathelement(location: new File(repoDir, 'org/mortbay/jetty/jsp-2.1/6.0.1/jsp-2.1-6.0.1.jar'))
            pathelement(location: new File(repoDir, 'org/mortbay/jetty/jetty-util/6.0.1/jetty-util-6.0.1.jar'))
            pathelement(location: new File(repoDir, 'javax/servlet/servlet-api/2.4/servlet-api-2.4.jar'))
            pathelement(location: new File(repoDir, 'javax/management/jmxri/1.2.1/jmxri-1.2.1.jar'))
            pathelement(location: new File(repoDir, 'javax/management/jmxremote/1.0.1_04/jmxremote-1.0.1_04.jar'))
            pathelement(location: new File(repoDir, 'javax/management/jmxremote_optional/1.0.1_04/jmxremote_optional-1.0.1_04.jar'))
            pathelement(location: new File(repoDir, 'org/beanshell/bsh/2.0b4/bsh-2.0b4.jar'))
            pathelement(location: new File(repoDir, 'berkleydb/je/3.2.13/je-3.2.13.jar'))
            pathelement(location: new File(repoDir, 'commons-cli/commons-cli/1.0//commons-cli-1.0.jar'))
            pathelement(location: new File(repoDir, 'commons-io/commons-io/1.2/commons-io-1.2.jar'))
            pathelement(location: new File(repoDir, 'commons-collections/commons-collections/3.1/commons-collections-3.1.jar'))
            pathelement(location: new File(repoDir, 'commons-lang/commons-lang/2.0/commons-lang-2.0.jar'))
            pathelement(location: new File(repoDir, 'xmlbeans/xbean/2.1.0/xbean-2.1.0.jar'))
            pathelement(location: new File(repoDir, 'stax/stax-api/1.0.1/stax-api-1.0.1.jar'))
            pathelement(location: new File(repoDir, 'log4j/log4j/1.2.9/log4j-1.2.9.jar'))
            pathelement(location: new File(repoDir, 'concurrent/concurrent/1.3.4/concurrent-1.3.4.jar'))
            pathelement(location: new File(repoDir, 'trove/trove/1.1-beta-5/trove-1.1-beta-5.jar'))
            pathelement(location: new File(repoDir, 'knopflerfish-tc/knopflerfish-tc/2.0.1/knopflerfish-tc-2.0.1.jar'))
}
ant.property(name:'tcp', refid: "cp")
def tcPath=ant.antProject.getProperty('tcp')

command.properties['tc.install-root'] = "${installRoot}"
command.properties['tc.config'] = "${installRoot}/tc-config-geronimo.xml"
command.properties['tc.classpath'] = tcPath
