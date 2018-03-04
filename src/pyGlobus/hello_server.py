#!//usr/local/bin/python 

from pyGlobus.io import GSISOAP
import sys, exceptions

def sayHello(s, _SOAPContext):
    print "hit the submit method"
    s = "pyGlobus says hello to " + s
    return s

port = 8080
if len(sys.argv) > 1:
    port = int(sys.argv[1])
print "starting pyGlobus hello server on ", port
server = GSISOAP.SOAPServer(("localhost", port))
server.registerFunction(GSISOAP.MethodSig(sayHello,keywords=0, context=1), "urn:hello:sample")
server.serve_forever()
