package de.bb.mejb;

import java.rmi.RemoteException;
import javax.ejb.*;

public class SessionHomeBean extends SimpleHomeBean implements EJBHome {

    protected SessionHomeBean() throws RemoteException {
        super();
    }

    public void remove(Handle handle) throws RemoteException, RemoveException {
    }

    public void remove(Object obj) throws RemoteException, RemoveException {
    }
}
