import java.lang.System
import terracotta

webappDir = java.lang.System.getProperty('webapp.dir')

appUtil = terracotta.AppUtil(AdminApp, webappDir)
appUtil.installAll()

if AdminConfig.hasChanges():
        AdminConfig.save()
