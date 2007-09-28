def installRoot = new File(command.geronimoHome, 'var/terracotta')
command.javaFlags << "-Xbootclasspath/p:\"${installRoot}/boot.jar\""
