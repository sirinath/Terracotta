import java.lang.System
import terracotta
import os

webappDir = os.environ["WAS_SANDBOX"] + '\\' + os.environ["PORT"] + '\\webapps'

print "Got webapp.dir: " + webappDir

appUtil = terracotta.AppUtil(AdminApp, webappDir)
appUtil.installAll()

if AdminConfig.hasChanges():
    AdminConfig.save()
