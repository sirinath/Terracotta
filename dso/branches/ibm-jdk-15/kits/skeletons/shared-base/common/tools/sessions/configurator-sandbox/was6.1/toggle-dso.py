import java.lang.Boolean
import terracotta

enableDso = java.lang.Boolean.getBoolean("enable.dso")
dso = terracotta.DSO(AdminTask)

if enableDso and not dso.isEnabled():
    dso.enable()
elif not enableDso and dso.isEnabled()
    dso.disable()

if AdminConfig.hasChanges():
    AdminConfig.save()
