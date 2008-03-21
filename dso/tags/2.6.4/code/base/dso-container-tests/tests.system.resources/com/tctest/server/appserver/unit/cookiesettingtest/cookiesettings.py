# this file is only used with CookieSettingTest
import operator

server = AdminConfig.getid('/Server:server1/')
smgr = AdminConfig.list('SessionManager', server)
AdminConfig.modify(smgr, [['enableCookies', 'true'], ['defaultCookieSettings',  [['name', 'CUSTOMSESSIONID'], ['maximumAge', 3600], ['domain', 'localhost'], ['path', '/CookieSettingTest'], ['secure', 'true']]]])
AdminConfig.save()
