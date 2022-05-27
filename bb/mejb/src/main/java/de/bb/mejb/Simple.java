package de.bb.mejb;

import java.rmi.RemoteException;
import javax.ejb.EJBObject;

public interface Simple extends EJBObject {

    public abstract void ejbStore() throws RemoteException;
}
