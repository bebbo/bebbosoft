package de.bb.mejb;

import de.bb.rmi.EasyRemoteObject;
import java.rmi.RemoteException;
import javax.ejb.*;

abstract class SimpleHomeBean extends EasyRemoteObject implements EJBHome {

    public EJBMetaData getEJBMetaData() throws RemoteException {
        return null;
    }

    public HomeHandle getHomeHandle() throws RemoteException {
        return null;
    }
}
