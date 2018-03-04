package interop.ogsa;

public interface Hello extends soaprmi.Remote {
    public int hello(String value) throws soaprmi.RemoteException;
}
