package interop.ogsa;

import org.gjt.xpp.XmlNode;
import org.gjt.xpp.XmlStartTag;
import org.gjt.xpp.XmlTag;
import org.gjt.xpp.XmlPullParser;
import org.gjt.xpp.XmlPullParserFactory;

import java.io.StringWriter;
import java.io.StringReader;
import java.util.Enumeration;

// this is a simple client that can be used to interogate Grid Services that are built by ogsa-3.0.
// the only argument is the gridservice handle of the grid service.  For example
// run.sh interop.ogsa.Main http://129.79.246.124:8089/ogsa/services/core/registry/ContainerRegistryService
// will run against the container registry service in ogsa-3.0 core which was launched as
// java org.globus.ogsa.server.ServiceContainer -p 8089

public class Main {

  public static void main (String args[])
    throws Exception {

    soaprmi.soaprpc.HttpSocketSoapInvoker hsi =
      (soaprmi.soaprpc.HttpSocketSoapInvoker)
      soaprmi.soaprpc.HttpSocketSoapInvoker.getDefault();
    hsi.setDefaultTimeout(1 * 60 * 1000); // 1 minute
    soaprmi.soaprpc.SoapServices.getDefault().setInvoker(hsi);

    String location = args.length > 0 ? args[0] : null;

    soaprmi.mapping.XmlJavaMapping mapping =
      soaprmi.soap.Soap.getDefault().getMapping();
    mapping.setDefaultStructNsPrefix(null);
    String namespace = "http://www.gridforum.org/namespaces/2003/03/OGSI";
    String soapAction = "http://www.gridforum.org/namespaces/2003/03/OGSI#findServiceData";
    
    GridServicePort ogsaServer = (GridServicePort)
      soaprmi.soaprpc.SoapServices.getDefault().
      createStartpoint(location,
		       new Class[]{GridServicePort.class},
		       namespace,
		       soaprmi.soap.SoapStyle.IBMSOAP,
		       soapAction);

    // prepare query
    // the "name" value is set here to the standard value "serviceDataName" which should return
    // all the other valid names that can be used.  other good ones to try are:
    //  entry - spills the contents of the Container Register Service
    //  gridServicehandle
    //  factoryLocator
    //  interface
    //  setServiceDataExtensibility
    //  terminationTime
    //  gridServiceReference
    //  findServiceDataExtensibility
    String queryString =
       "<ogsi:queryExpression xmlns:ogsi=\"http://www.gridforum.org/namespaces/2003/03/OGSI\">"
    +  "<ogsi:queryByServiceDataNames>"
    +     "<ogsi:name>serviceDataName</ogsi:name>"
    +   "</ogsi:queryByServiceDataNames>"
   +   "</ogsi:queryExpression>";

    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
    XmlPullParser pp = factory.newPullParser();
    pp.setInput(new StringReader(queryString));
    pp.setNamespaceAware(true);
    pp.next();
    XmlNode query = factory.newNode(pp);

    XmlNode response = ogsaServer.findServiceData(query);
    
    // get response
    XmlNode serviceData;
    for(int i = 0; i < response.getChildrenCount(); i++){
        if(response.getChildAt(i).getClass().getName() == "org.gjt.xpp.impl.node.Node"){
            serviceData = (XmlNode) response.getChildAt(i);
            if(serviceData.getLocalName().equals("serviceDataValues")){
                System.out.println("-------------------- serviceDataValues -------------------------------");
                Enumeration vals = serviceData.children();
                while(vals.hasMoreElements()){
                    Object elem = vals.nextElement();
                    if(elem.getClass().getName() == "org.gjt.xpp.impl.node.Node"){
                        XmlNode element = (XmlNode) elem;
                        System.out.println("Element : "+ element.getLocalName());
                        StringWriter sw = new StringWriter();
                        factory.writeNode(element, sw, true);
                        System.out.println(sw.toString());
                        System.out.println("----------------------------------------------");
                        }
                }
                System.out.println("---------------  end serviceDataValues -------------------------------");
            }
       }
    }
  }
}
