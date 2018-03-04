<%@ page import = "hello.*" %>
<html>
<body bgcolor="white">

<h1>Hello Service!</h1>

<p>
<%
String name = request.getParameter("name");
String pageUri = HttpUtils.getRequestURL(request).toString();
String SERVLET = "/servlet/hello/hello-service";
//String serviceLocation = "http://lccalhost:8080/soaprmi"+SERVLET;
String serviceLocation = "http://"
	      +request.getServerName()+":"+request.getServerPort()
	      +"/xsoap" + SERVLET;

if(pageUri != null) {
  int pos = pageUri.lastIndexOf('/');
  String soaprmiUri = pageUri;
  if(pos != -1) {
    soaprmiUri = pageUri.substring(0, pos);
  }
  serviceLocation = soaprmiUri + SERVLET;
}
if(name != null) {
%>
Invoking <a href="<%=serviceLocation%>">hello service</a> with name='<%=name%>'
<% 
HelloService serverRef = (HelloService) 
       soaprmi.soaprpc.SoapServices.getDefault().createStartpoint(
	 serviceLocation,
         new Class[]{HelloService.class}
       );       
String greeting = serverRef.sayHello(name);
%>
<P>Received <%=greeting%>.
<%
}
%>

<P>
<form method="GET">
Send greeting to <a href="<%=serviceLocation%>">hello service</a>:
<input type="text" name="name" value="<%=name!=null?name:"World!"%>">
<input type="submit">
</form>


</html>
