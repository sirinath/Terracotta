def installRoot = new File(command.geronimoHome, 'var/terracotta')
def bootJar = new File(installRoot, 'lib/dso-boot/dso-boot.jar')
if (!bootJar.exists()){
    def ant = new AntBuilder()
    def repoDir = new File(command.geronimoHome, 'repository')
    def tcConfig = new File(installRoot, 'tc-config-geronimo.xml')
    ant.path(id: "cp"){
      ant.fileset(dir:repoDir){
        include(name:"**/*.jar")
      }
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
        sysproperty(key:"tc.tests.configuration.modules.url", value:repoDir)
    }   
}   

command.javaFlags << "-Xbootclasspath/p:${bootJar}"
