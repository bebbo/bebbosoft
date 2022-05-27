package de.bb.mejb;

import de.bb.rmi.EasyRemoteObject;
import java.rmi.RemoteException;
import javax.ejb.*;

public abstract class SBean extends EasyRemoteObject implements EJBObject, SessionBean {

    protected SBean(SessionHomeBean sessionhomebean) throws EJBException, RemoteException {
        Code = sessionhomebean;
        setSessionContext(new SC(sessionhomebean, this));
    }

    public EJBHome getEJBHome() throws RemoteException {
        return Code;
    }

    public Object getPrimaryKey() throws RemoteException {
        return null;
    }

    public void remove() throws RemoteException, RemoveException {
    }

    public Handle getHandle() throws RemoteException {
        return null;
    }

    public boolean isIdentical(EJBObject ejbobject) throws RemoteException {
        return false;
    }

    public void ejbRemove() throws EJBException, RemoteException {
    }

    public void ejbActivate() throws EJBException, RemoteException {
    }

    public void ejbPassivate() throws EJBException, RemoteException {
    }

    private SessionHomeBean Code;
}
