from pyGlobus.io import GSISOAP
from pyGlobus.util.globusException import GlobusException
import sys, traceback

try:
    location = "https://localhost:8080"
    if len(sys.argv) > 1:
        location = sys.argv[1]
    #
    print "Connecting to ", location
    server = GSISOAP.SOAPProxy(location, namespace="urn:hello:sample")
    #server = GSISOAP.SOAPProxy("https://localhost:8080", namespace="urn:hello:sample")
    #server = SOAP.SOAPProxy("http://binkley.lbl.gov/soap/servlet/rpcrouter", namespace="urn:gtg-Echo")
    print server.sayHello("spam, spam, spam, eggs, and spam")
except GlobusException, ex:
    print ex.msg
