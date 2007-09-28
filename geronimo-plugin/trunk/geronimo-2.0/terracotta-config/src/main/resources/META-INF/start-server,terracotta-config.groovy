def installRoot = new File(command.geronimoHome, 'var/terracotta')
command.properties['tc.install-root'] = "${installRoot}"
command.properties['tc.config'] = "${installRoot}/tc-config-geronimo.xml"
