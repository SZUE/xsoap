package interop.ogsa;

import org.gjt.xpp.XmlNode;

public interface GridServicePort extends soaprmi.Remote {
  public XmlNode findServiceData(XmlNode xmlAsTree)
    throws soaprmi.RemoteException;
}
