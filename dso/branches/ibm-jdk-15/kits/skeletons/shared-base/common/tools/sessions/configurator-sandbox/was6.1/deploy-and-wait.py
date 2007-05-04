
import time.sleep
import java.lang.System

_profile = java.lang.System.getProperty('profileName')
_warDir  = java.lang.System.getProperty('warDirectory')

################################################################################
## Helper functions
################################################################################

def _info(s):
    print '[INFO]  ' + s

def _error(s):
    print '[ERROR] ' + s

#def _deployWar(warFile):
#    print 'YES!'

################################################################################
## Main program
################################################################################

try:
    serverSearchString = 'type=Server,j2eeType=J2EEServer,node=' + AdminControl.getNode() + ',cell=' + AdminControl.getCell() + ',*'
    _serverInstances = AdminControl.queryMBeans(serverSearchString)
    if _serverInstances.size() != 1:
        _error('Found ' + str(_serverInstances.size()) + ' servers, expected 1: ' + str(_serverInstances))
        sys.exit(1)
    _serverInstance = _serverInstances[0]
    _serverName     = _serverInstance.getObjectName()
    _serverProcess  = _serverName.getKeyProperty('process')
    _info('Connected to WebSphere Application Server process[' + _serverProcess + '], waiting for shutdown.')
    _connected          = 'True'
    _stoppingStateFound = None
    while 1:
            _serverState = AdminControl.getAttribute(_serverName.toString(), "state")
            if _serverState == 'STOPPED':
                _info('Server is stopped')
                sys.exit(0)
            elif _serverState == 'STOPPING':
                _stoppingStateFound = 'True'
            sleep(3)
except:
    if _stoppingStateFound:
        _info('Server is stopped')
    elif _connected:
        _error('Lost connection to server before we were able to see a STOPPING state, assuming server is now stopped')
    else:
        _error('Unable to connect to server ' + _server + ', assuming it is stopped')
    sys.exit(0)
