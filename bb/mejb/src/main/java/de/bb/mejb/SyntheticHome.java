package de.bb.mejb;

import java.rmi.RemoteException;

import javax.ejb.Handle;
import javax.ejb.RemoveException;

public class SyntheticHome extends SimpleHomeBean {

    public SyntheticHome() {
    }

    public void remove(Handle handle) throws RemoteException, RemoveException {
    }

    public void remove(Object obj) throws RemoteException, RemoveException {
    }
}
