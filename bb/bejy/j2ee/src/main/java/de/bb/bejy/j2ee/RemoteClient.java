package de.bb.bejy.j2ee;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.SoftReference;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import javax.naming.Context;
import javax.naming.NamingException;

import de.bb.util.Pool;

/**
 * @author bebbo
 * 
 */
public class RemoteClient {
    private static boolean addedShutdownHook;

    // holds all clients, if more than one rmi connection is used.
    static HashMap<Long, RemoteClient> clients = new HashMap<Long, RemoteClient>();

    // our own id
    long cid;

    // used to release objects on the server
    private ArrayList<Long> freeList = new ArrayList<Long>();

    private Pool connections;

    private LongHash<SoftReference<RemoteRef>> refs = new LongHash<SoftReference<RemoteRef>>();

    /**
     * creates the client object which maintains the connection to the server.
     * 
     * @param properties
     * @throws NamingException
     */
    RemoteClient(Hashtable<?, ?> properties, Principal principal) throws NamingException {
        String dest = (String) properties.get(Context.PROVIDER_URL);
        dest = dest.substring(10);
        int port = 1111;
        int ldot = dest.lastIndexOf(':');
        if (ldot > 0) {
            port = Integer.parseInt(dest.substring(ldot + 1));
            dest = dest.substring(0, ldot);
        }

        connections = new Pool(new StreamFactory(dest, port));

        Streams s = null;
        try {
            s = getStreams();
            ObjectOutputStream os = s.os;
            os.writeByte(Constants.CMD_CONNECT);
            os.writeObject(principal);
            //      os.writeUTF(user);
            //      os.writeUTF(pwd);
            os.flush();

            ObjectInputStream is = s.is;
            //byte err = 
            is.readByte();
            cid = is.readLong();

            clients.put(new Long(cid), this);

            if (!addedShutdownHook) {
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    /**
                     * (non-Javadoc)
                     * 
                     * @see java.lang.Thread#run()
                     */
                    public void run() {
                        try {
                            Streams ss = getStreams();
                            ss.os.writeByte(Constants.CMD_TERMINATE);
                            ss.os.writeLong(cid);
                            ss.os.flush();
                        } catch (Exception e) {
                        }
                    }
                });
                addedShutdownHook = true;
            }
        } catch (Exception ex) {
            throw new NamingException(ex.getMessage());
        } finally {
            releaseStreams(s);
        }
    }

    /**
     * obtains a free set of in/out streams.
     * 
     * @return
     * @throws Exception
     */
    synchronized Streams getStreams() throws Exception {
        Streams ss = (Streams) connections.obtain(this);
        ss.os.reset();
        if (freeList.size() > 0)
            synchronized (freeList) {
                if (freeList.size() > 0) {
                    //ss.os.reset();
                    ss.os.writeByte(Constants.CMD_GC);
                    ss.os.writeLong(cid);

                    //          ss.os.writeObject(freeList);
                    ss.os.writeInt(freeList.size());
                    for (int i = freeList.size(); --i >= 0;) {
                        ss.os.writeLong(freeList.get(i).longValue());
                    }

                    // freeList.clear() is buggy!
                    freeList = new ArrayList<Long>();
                }
            }
        //    if ((++ss.useCount & 0x3f) == 1)
        //ss.os.reset();
        return ss;
    }

    /**
     * checks whether remote gc object should be send.
     * 
     * @throws Exception
     */
    void checkGC() throws Exception {
        if (freeList.size() > 0)
            synchronized (freeList) {
                if (freeList.size() > 0) {
                    Streams ss = (Streams) connections.obtain(this);
                    try {

                        //ss.os.reset();
                        ss.os.writeByte(Constants.CMD_GC);
                        ss.os.writeLong(cid);

                        ss.os.writeInt(freeList.size());
                        for (int i = freeList.size(); --i >= 0;) {
                            ss.os.writeLong(freeList.get(i).longValue());
                        }

                        //          ss.os.writeObject(freeList);
                        ss.os.flush();
                        // freeList.clear() is buggy, since the object stream remembers the object, but not the content!
                        freeList = new ArrayList<Long>();
                    } finally {
                        connections.release(this, ss);
                    }
                }
            }
    }

    void releaseStreams(Streams s) {
        if (s != null) {
            try {
                s.is.available();
            } catch (IOException e) {
                try {
                    s = (Streams) connections.renew(this);
                } catch (Exception e1) {
                }
            }
            connections.release(this, s);
        }
    }

    /**
     * @param name
     * @return
     * @throws NamingException
     */
    public Object lookup(String name) throws NamingException {
        Streams s = null;
        try {
            s = getStreams();

            ObjectOutputStream os = s.os;
            os.writeByte(Constants.CMD_CREATE);
            os.writeLong(cid);
            os.writeUTF(name);
            os.flush();

            ObjectInputStream is = s.is;
            Object ref = receiveObject(is);
            return ref;
        } catch (Exception ex) {
            NamingException ne = new NamingException(ex.getMessage());
            ne.initCause(ex);
            throw ne;
        } finally {
            releaseStreams(s);
        }
    }

    /**
     * @param className
     * @param oid
     * @param fx
     * @return
     * @throws RemoteException
     */
    public Object invoke(String className, long oid, int fx) throws RemoteException {
        Streams s = null;
        try {
            s = getStreams();

            ObjectOutputStream os = s.os;
            os.writeByte(Constants.CMD_INVOKE);
            os.writeLong(cid);
            os.writeUTF(className);
            os.writeLong(oid);
            os.writeShort(fx);

            os.writeByte(0);
            os.flush();

            ObjectInputStream is = s.is;
            Object ret = receiveObject(is);
            return ret;
        } catch (Exception ex) {
            //      ex.printStackTrace();
            try {
                s = (Streams) connections.renew(this);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (!(ex instanceof RemoteException))
                ex = new RemoteException(ex.getMessage(), ex);
            throw (RemoteException) ex;
        } finally {
            releaseStreams(s);
        }
    }

    /**
     * @param className
     * @param oid
     * @param fx
     * @param params
     * @return
     * @throws RemoteException
     */
    public Object invoke(String className, long oid, int fx, Object[] params) throws RemoteException {
        //    System.out.println(className + oid + ":" + fx);

        Streams s = null;
        try {
            s = getStreams();

            ObjectOutputStream os = s.os;
            os.writeByte(Constants.CMD_INVOKE);
            os.writeLong(cid);
            os.writeUTF(className);
            os.writeLong(oid);
            os.writeShort(fx);
            os.writeByte(params.length);
            for (int i = 0; i < params.length; ++i) {
                Marshal.writeObject(params[i], os);
            }
            os.flush();

            ObjectInputStream is = s.is;
            Object ret = receiveObject(is);
            return ret;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RemoteException(ex.getMessage());
        } finally {
            releaseStreams(s);
        }
    }

    /**
     * Method receiveObject.
     * 
     * @param is
     * @return Object
     * @throws Exception
     */
    private Object receiveObject(ObjectInputStream is) throws Exception {
        //OIS ois = (OIS) is;
        byte err = is.readByte();

        while (err == Constants.OIS_STUB) {
            String className = is.readUTF();
            byte b[] = (byte[]) is.readObject();
            CL.cl.loadClass(className, b);

            err = is.readByte();
        }

        if (err == Constants.OIS_EXCEPTION) {
            RemoteException r = (RemoteException) is.readObject();
            throw r;
        }

        if (err == Constants.OIS_SERIALIZED) {
            return Marshal.readObject(is);
        }

        String className = is.readUTF();
        long oid = is.readLong();
        Class<?> clazz = CL.cl.loadClass(className + "_Ying");
        RemoteRef ref = (RemoteRef) clazz.newInstance();
        ref.initialize(this, oid);
        return ref;
    }

    /**
     * Method release.
     * 
     * @param oid
     */
    void release(long oid) {
        // System.out.println("free: " + oid);
        refs.remove(oid);
        freeList.add(new Long(oid));
    }

    void put(long oid, RemoteRef ref) {
        refs.put(oid, new SoftReference<RemoteRef>(ref));
    }

    RemoteRef get(long oid) {
        SoftReference<RemoteRef> sr = refs.get(oid);
        if (sr == null)
            return null;
        return sr.get();
    }

    /**
     * Method getClient.
     * 
     * @param cid
     * @return Client
     */
    static RemoteClient getClient(long cid) {
        return clients.get(new Long(cid));
    }

    /**
     * Method loadStub.
     * 
     * @param className
     * @return
     * @throws Exception
     */
    Class<?> loadStub(String className) throws Exception {
        Streams s = null;
        try {
            int max = connections.getMaxCount();
            connections.setMaxCount(max + 100);
            s = (Streams) connections.obtain(className);
            connections.setMaxCount(max);

            s.os.writeByte(Constants.CMD_LOADSTUB);
            s.os.writeLong(cid);
            s.os.writeUTF(className);
            s.os.flush();
            byte err = s.is.readByte();
            if (err == Constants.OIS_STUB) {
                String className2 = s.is.readUTF();
                if (!className.equals(className2))
                    throw new Exception("class stub mismatch");
                byte b[] = (byte[]) s.is.readObject();

                return CL.cl.loadClass(className, b);
            }

            throw new RemoteException("cannot load " + className);
        } finally {
            connections.release(className, s);
        }
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.gc();
                for (Iterator<RemoteClient> i = clients.values().iterator(); i.hasNext();) {
                    RemoteClient c = i.next();
                    try {
                        c.checkGC();
                    } catch (Exception e) {
                    }
                }
            }
        });
    }

    /**
     * @param cn
     * @return
     * @throws ClassNotFoundException
     */
    Class<?> loadClass(String cn) throws ClassNotFoundException {
        return CL.cl.loadClass(cn);
    }
}