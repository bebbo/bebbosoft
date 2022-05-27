package de.bb.mejb;

import java.rmi.RemoteException;
import javax.ejb.*;

class EC extends C implements EntityContext {

    EC(CMPHomeBean cmphomebean, CMPBean cmpbean) {
        super(cmphomebean);
        Code = cmpbean;
    }

    public EJBLocalObject getEJBLocalObject() throws IllegalStateException {
        return null;
    }

    public EJBObject getEJBObject() throws IllegalStateException {
        return Code;
    }

    public Object getPrimaryKey() throws IllegalStateException {
        try {
            return Code.getPrimaryKey();
        } catch (RemoteException remoteexception) {
            IllegalStateException illegalstateexception = new IllegalStateException();
            illegalstateexception.initCause(remoteexception);
            throw illegalstateexception;
        }
    }

    CMPBean Code;
}
