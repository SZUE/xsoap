Before running sample make sure to obtain pyGlobus from 
http://www-itg.lbl.gov/grid/projects/pyGlobus/

By default pyGlobus will do delegation however
XSOAP by default ids not doing delegation 
(as right now we do not support passing delegation to invoked service).

NOTE: to connect to pyGlobus services use XSOAP cog_delegation
security provider for example by setting system property 
soaprmi.security.providers to cog_delegation 
ie. -Dsoaprmi.security.providers=cog_delegation


----------------------------------------------------------------
To run this sample:

first make sure to set correctly $PYTHONPATH to point to pyGlobus

then to talk to pyGlobus sample server that requires delegation:

* XSOAP client with delegation --> pyGLobus server with delegation 

start pyGlobus server:

[aslom@let pyGlobus]$ python hello_server.py 
starting pyGlobus hello server on  8080
bound to port  8080
GSI server listening on 8080
inside GSITCPServer.auth_callback. Remote user is /C=US/O=Globus/O=Indiana University/OU=Extreme Lab/CN=Aleksander Andrzej Slominski
hit the submit method
let.extreme.indiana.edu - - [03/Nov/2001 23:27:15] "POST / HTTP/1.0" 200 -

and connect with XSOAP clien but first GSI delegation must be enabled:

export JAVA_OPTS=-Dsoaprmi.security.providers=cog_delegation

then 
[aslom@let xsoap-java]$ run.sh hello_client https://localhost:8080
/usr/java/jdk1.3.1_01/bin/java -Dsoaprmi.security.providers=cog_delegation -cp build/samples:build/classes:build/tests:lib/cog/cog-0_9_12.jar:lib/cog/cryptix.jar:lib/cog/iaik_jce_full.jar:lib/cog/iaik_ssl.jar:lib/servlet_api/servlet.jar:lib/junit/junit37.jar:. hello.HelloClient https://localhost:8080
Client executing remote method sayHello on server with 'World' argument
...
Server said 'pyGlobus says hello to World'


NOTE: XSOAP client can connect with delegation set to NONE
export JAVA_OPTS=-Dsoaprmi.security.providers=cog_delegation_none


* pyGlobus client with delegation --> XSOAP server with delegation

start XSOAP server with delegation first set system property:
export JAVA_OPTS=-Dsoaprmi.security.providers=cog_delegation
than start server:

[aslom@let xsoap-java]$ run.sh hello_server -secure 8080

and use pyGlobus to connect to it:

[aslom@let pyGlobus]$ python hello_client.py 
Connecting to  https://localhost:8080
Expected name is /C=US/O=Globus/O=Indiana University/OU=Extreme Lab/CN=Aleksander Andrzej Slominski
name is /C=US/O=Globus/O=Indiana University/OU=Extreme Lab/CN=Aleksander Andrzej Slominski
Hello from Indiana to spam, spam, spam, eggs, and spam!


-------------------------------------------------------
And when use of delegation is disabled in pyGlobus 
[see below how to disbale delegation in pyGlobus]:

* XSOAP client no delegation --> pyGLobus server no delegation 

start pyGlobus server:

[aslom@let pyGlobus]$ python hello_server.py 
starting pyGlobus hello server on  8080
bound to port  8080
GSI server listening on 8080
inside GSITCPServer.auth_callback. Remote user is /C=US/O=Globus/O=Indiana Unive
rsity/OU=Extreme Lab/CN=Aleksander Andrzej Slominski
hit the submit method
let.extreme.indiana.edu - - [03/Nov/2001 22:49:32] "POST / HTTP/1.0" 200 -


and then connect using XSOAP client:
(system property soaprmi.security.providers should have value cog or be not set:
export JAVA_OPTS=-Dsoaprmi.security.providers=cog)


[aslom@let xsoap-java]$ run.sh hello_client https://localhost:8080
/usr/java/jdk1.3.1_01/bin/java -cp build/samples:build/classes:build/tests:lib/cog/cog-0_9_12.jar:lib/cog/cryptix.jar:lib/cog/iaik_jce_full.jar:lib/cog/iaik_ssl.jar:lib/servlet_api/servlet.jar:lib/junit/junit37.jar:. hello.HelloClient https://localhost:8080
Client executing remote method sayHello on server with 'World' argument
...
Server said 'pyGlobus says hello to World'


and finally to test last possible configuration:

* pyGlobus client no delegation --> pyGLobus server no delegation 

start XSOAP server:

[aslom@let xsoap-java]$ run.sh hello_server -secure 8080
...
Server waiting for connections...


and connect with pyGlobus client:

[aslom@let pyGlobus]$ python hello_client.py 
Connecting to  https://localhost:8080
Expected name is /C=US/O=Globus/O=Indiana University/OU=Extreme Lab/CN=Aleksander Andrzej Slominski
name is /C=US/O=Globus/O=Indiana University/OU=Extreme Lab/CN=Aleksander Andrzej Slominski
Hello from Indiana to spam, spam, spam, eggs, and spam!



-------------
How to disable delegation in pyGlobus/

It is possible to run pyGlobus in non delegation mode and talk with
default non delefating CoG security provider you will need to do this:
(thanks to Keith Jackson for those explanations!):

"I think it will work fine if you change the line in GSIhttplib.py from
  self.io_attr.set_channel_mode(ioc.GLOBUS_IO_SECURE_CHANNEL_MODE_CLEAR) 
to:
  self.io_attr.set_channel_mode(ioc.GLOBUS_IO_SECURE_CHANNEL_MODE_GSI_WRAP)
if that doesn't work try:
  self.io_attr.set_channel_mode(ioc.GLOBUS_IO_SECURE_CHANNEL_MODE_SSL_WRAP).
  
To turn off delegation in the client change the following line in GSIhttplib.py from:
  self.io_attr.set_delegation_mode(ioc.GLOBUS_IO_SECURE_DELEGATION_MODE_FULL_PROXY)
to:
  self.io_attr.set_delegation_mode(ioc.GLOBUS_IO_SECURE_DELEGATION_MODE_NONE)"


i have tested following setup: for non delegation to talk 
with default XSOAP CoG security provider:

in GSIhttplib.py
  self.io_attr.set_channel_mode(ioc.GLOBUS_IO_SECURE_CHANNEL_MODE_SSL_WRAP)
 
in GSITCPServer.py
   self.attr.set_channel_mode(ioc.GLOBUS_IO_SECURE_CHANNEL_MODE_SSL_WRAP)
   
(it seems that SSL_WRAP disables any delegation attempts 
so setting self.io_attr.set_delegation_mode(...) is ignored)


   
and to talk with XSOAP with delegatione nabled:

in GSIhttplib.py
  self.io_attr.set_channel_mode(ioc.GLOBUS_IO_SECURE_CHANNEL_MODE_GSI_WRAP)

(setting self.io_attr.set_delegation_mode(...) will decide if
'0' for no delegation or 'D' for delegation is sent in client)


in GSITCPServer.py
   self.attr.set_channel_mode(ioc.GLOBUS_IO_SECURE_CHANNEL_MODE_GSI_WRAP)
