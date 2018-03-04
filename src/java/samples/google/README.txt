This client shows how to access the google webservice using XSOAP.
to make this work you must edit the Client code and place you secret
key from google in the key variable.  see http://www.google.com/apis
for instructions on getting a key. Alternatively the google key
can be put into google_key.txt file and changed without need
to recompile Java source code (put your google key into
sample propery file google_key.txtx provided in this directory).

also note: the castor generated classes have been edited to work with
xsoap.  In file GoogleSearchResults.xsd the schema has been modified
from the original so that the arrays are not included (to make castor
happy).  then the generated code was modified so that SearchElement
and DirectoryCategory return arrays.  also abstract methods and
class attributes were removed.  XSOAP can then generate the correct
code if the parameter and classes are correctly mapped.  see fixNames
in Client.java and XSOAP FAQ entry in doc/faq.html (available also online at
http://www.extreme.indiana.edu/xgws/xsoap/download/xsoap_1_2/doc/faq.html#map_params).


NOTE: if you do not change source code or google_key.txt file to
include google secret key the search will not work and
you will get unauthorized key exception from google service, ex:

>run google.Client xsoap (on UN*X use ./run.sh google.Client xsoap)
JAVA_HOME=c:\jdk1.3
c:\jdk1.3\bin\java   -cp build\classes;build\samples;build\tests;lib\cog\iaik_ssl.jar;lib\cog\iaik_j
ce_full.jar;lib\cog\cryptix.jar;lib\cog\cog-20011108.jar;lib\jsse\jsse.jar;lib\jsse\jnet.jar;lib\jss
e\jcert.jar;lib\wsdl\wsdl4j_0_8.jar;lib\servlet_api\servlet22.jar;lib\junit\junit37.jar;  google.Cli
ent  xsoap
contacting gooogle service $Proxy0@11513e61 to Port[name=,portType=PortType[uri=urn:soaprmi-v11:temp
-java-port-type,name=google.GoogleSearch],endpoint=Endpoint[location=http://api.google.com:80/search
/beta2,binding=soaprmi.port.SoapBinding@37697,cookie=null]]
Exception in thread "main" soaprmi.ServerException: SOAP-ENV:Server: Exception from service object:
Invalid authorization key: --------------------------------
        at soaprmi.soaprpc.MethodInvoker.receiveResponse(MethodInvoker.java:439)
        at soaprmi.soaprpc.HttpSocketSoapInvocationHandler.invokeTransport(HttpSocketSoapInvocationH
andler.java:135)
        at soaprmi.soaprpc.SoapDynamicStub.invoke(SoapDynamicStub.java:120)
        at $Proxy0.doGoogleSearch(Unknown Source)
        at google.Client.main(Client.java:59)
remote exception is: com.google.soap.search.GoogleSearchFault: Invalid authorization key: ----------
----------------------
        at com.google.soap.search.QueryLimits.lookUpAndLoadFromINSIfNeedBe(QueryLimits.java:220)
        at com.google.soap.search.QueryLimits.validateKey(QueryLimits.java:127)
        at com.google.soap.search.GoogleSearchService.doPublicMethodChecks(GoogleSearchService.java:
808)
        at com.google.soap.search.GoogleSearchService.doGoogleSearch(GoogleSearchService.java:113)
        at java.lang.reflect.Method.invoke(Native Method)
        at org.apache.soap.server.RPCRouter.invoke(Unknown Source)
        at org.apache.soap.providers.RPCJavaProvider.invoke(Unknown Source)
        at org.apache.soap.server.http.RPCRouterServlet.doPost(Unknown Source)
        at javax.servlet.http.HttpServlet.service(HttpServlet.java:760)
        at javax.servlet.http.HttpServlet.service(HttpServlet.java:853)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.j
ava:247)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:193)

        at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:243)
        at org.apache.catalina.core.StandardPipeline.invokeNext(StandardPipeline.java:566)
        at org.apache.catalina.core.StandardPipeline.invoke(StandardPipeline.java:472)
        at org.apache.catalina.core.ContainerBase.invoke(ContainerBase.java:943)
        at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:190)
        at org.apache.catalina.core.StandardPipeline.invokeNext(StandardPipeline.java:566)
        at org.apache.catalina.core.StandardPipeline.invoke(StandardPipeline.java:472)
        at org.apache.catalina.core.ContainerBase.invoke(ContainerBase.java:943)
        at org.apache.catalina.core.StandardContext.invoke(StandardContext.java:2343)
        at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:180)
        at org.apache.catalina.core.StandardPipeline.invokeNext(StandardPipeline.java:566)
        at org.apache.catalina.valves.ErrorDispatcherValve.invoke(ErrorDispatcherValve.java:170)
        at org.apache.catalina.core.StandardPipeline.invokeNext(StandardPipeline.java:564)
        at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:170)
        at org.apache.catalina.core.StandardPipeline.invokeNext(StandardPipeline.java:564)
        at org.apache.catalina.valves.AccessLogValve.invoke(AccessLogValve.java:468)
        at org.apache.catalina.core.StandardPipeline.invokeNext(StandardPipeline.java:564)
        at org.apache.catalina.core.StandardPipeline.invoke(StandardPipeline.java:472)
        at org.apache.catalina.core.ContainerBase.invoke(ContainerBase.java:943)
        at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:174)
        at org.apache.catalina.core.StandardPipeline.invokeNext(StandardPipeline.java:566)
        at org.apache.catalina.core.StandardPipeline.invoke(StandardPipeline.java:472)
        at org.apache.catalina.core.ContainerBase.invoke(ContainerBase.java:943)
        at org.apache.ajp.tomcat4.Ajp13Processor.process(Ajp13Processor.java:429)
        at org.apache.ajp.tomcat4.Ajp13Processor.run(Ajp13Processor.java:495)
        at java.lang.Thread.run(Thread.java:484)

