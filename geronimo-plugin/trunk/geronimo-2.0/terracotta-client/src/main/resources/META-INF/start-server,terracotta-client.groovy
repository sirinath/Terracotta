def installRoot = new File(command.geronimoHome, 'var/terracotta')
def bootJar = new File(installRoot, 'var/terracotta/dso-boot.jar')
if (!bootJar.exists()){
    def ant = new AntBuilder()
    def repoDir = new File(command.geronimoHome, 'repository')
    def tcConfig = new File(installRoot, 'tc-config-geronimo.xml')
    ant.java(classname: 'com.tc.object.tools.BootJarTool') {
        classpath {
            pathelement(location: new File(repoDir, 'org/terracotta/terracotta/2.5-SNAPSHOT/terracotta-2.5-SNAPSHOT.jar'))
            pathelement(location: new File(repoDir, 'commons-cli/commons-cli/1.0/commons-cli-1.0.jar'))
            pathelement(location: new File(repoDir, 'commons-io/commons-io/1.2/commons-io-1.2.jar'))
            pathelement(location: new File(repoDir, 'commons-lang/commons-lang/2.2/commons-lang-2.2.jar'))
            pathelement(location: new File(repoDir, 'org/apache/xmlbeans/xmlbeans/2.3.0/xmlbeans-2.3.0.jar'))
            pathelement(location: new File(repoDir, 'log4j/log4j/1.2.14/log4j-1.2.14.jar'))
            pathelement(location: new File(repoDir, 'concurrent/concurrent/1.3.4/concurrent-1.3.4.jar'))
            pathelement(location: new File(repoDir, 'org/terracotta/tcconfig2/2.0/tcconfig2-2.0.jar'))
            pathelement(location: new File(repoDir, 'org/terracotta/tcconfig1/1.0/tcconfig1-1.0.jar'))
            pathelement(location: new File(repoDir, 'org/terracotta/tcconfig/2.5-SNAPSHOT/tcconfig-2.5-SNAPSHOT.jar'))
            pathelement(location: new File(repoDir, 'stax/stax-api/1.0.1/stax-api-1.0.1.jar'))
            pathelement(location: new File(repoDir, 'trove/trove/1.1-beta-5/trove-1.1-beta-5.jar'))
            pathelement(location: new File(repoDir, 'knopflerfish-tc/knopflerfish-tc/2.0.1/knopflerfish-tc-2.0.1.jar'))
        }
        arg(value: '-v')
        arg(value: '-o')
        arg(value: bootJar)
        arg(value: '-f')
        arg(value: tcConfig)
        sysproperty(key:"geronimo-terracotta.home", value:tcConfig)
    }   
}   

command.javaFlags << "-Xbootclasspath/p:\"${bootJar}\""
