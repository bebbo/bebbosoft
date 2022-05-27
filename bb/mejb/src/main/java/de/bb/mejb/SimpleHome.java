package de.bb.mejb;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SimpleHome extends Remote {

    public abstract Simple internCreate() throws RemoteException;
}
