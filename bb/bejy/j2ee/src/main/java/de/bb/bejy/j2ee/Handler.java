/**
 * 
 */
package de.bb.bejy.j2ee;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;

class Handler extends Thread {
    private OIS is;

    private ObjectOutputStream os;

    ClientBean client;

    // private LZRWOutputStream zos;

    void setOutputStream(OutputStream _os) throws Exception {
        //      _os = new ZipOutputStream(_os);
        //      _os = new GZIPOutputStream(_os);
        //      _os = zos = new LZRWOutputStream(_os);

        _os = new BufferedOutputStream(_os, 1412);
        os = new ObjectOutputStream(_os);
        os.flush();
    }

    void setInputStream(InputStream _is) throws Exception {
        //      _is = new LZRWInputStream(_is);
        //      _is = new GZIPInputStream(_is);
        //      _is = new ZipInputStream(_is);
        _is = new BufferedInputStream(_is);
        is = new OIS(_is);
    }

    /**
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#run()
     */
    public void run() {
        try {
            //int n = 0;
            for (;;) {
                if (client != null)
                    client.last = System.currentTimeMillis();
                os.flush();
                int cmd = is.readByte() & 0xff;
                //if ((++n & 0x3f) == 0)
                os.reset();

                switch (cmd) {
                case Constants.CMD_CONNECT: {
                    Principal principal = (Principal) is.readObject();
                    //Properties properties = (Properties) is.readObject();
                    client = Registry.createClient(principal);
                    os.writeByte(0);
                    os.writeLong(client.cid);
                }
                    break;

                case Constants.CMD_LOADSTUB: {
                    Long cid = new Long(is.readLong());
                    String className = is.readUTF();
                    String cn = className.substring(0, className.length() - 5);

                    byte[] b = OnTheFly.mkstub(cn);
                    os.write(Constants.OIS_STUB);
                    os.writeUTF(className);
                    os.writeObject(b);

                    if (client == null) {
                        client = (ClientBean) Registry.clientBeans.get(cid);
                        if (client == null) {
                            os.write(Constants.OIS_EXCEPTION);
                            Exception ee = new RemoteException("unknown client id detected - please reconnect");
                            os.writeObject(ee);
                            break;
                        }
                    }
                    CL.cl.loadClass(cn + "_Ying", b);

                    b = OnTheFly.mkskel(cn);
                    CL.cl.loadClass(cn + "_Yang", b);

                }
                    break;
                case Constants.CMD_CREATE: {
                    Long cid = new Long(is.readLong());
                    String className = is.readUTF();

                    //if (client == null)
                    client = (ClientBean) Registry.clientBeans.get(cid);
                    if (client == null) {
                        os.write(Constants.OIS_EXCEPTION);
                        Exception ee = new RemoteException("unknown client id detected - please reconnect");
                        os.writeObject(ee);
                        break;
                    }
                    // check access

                    Object o = null;
                    try {
                        //                o = makeInstance(client.principal, client.helper, className);
                        o = Registry.makeInstance(client.principal, className);
                    } catch (Throwable t) {
                        os.write(Constants.OIS_EXCEPTION);
                        if (!(t instanceof RemoteException)) {
                            t = new RemoteException(t.getMessage(), t);
                        }
                        os.writeObject(t);
                        break;
                    }
                    //sendObject(client, o);
                    os.writeByte(Constants.OIS_SERIALIZED);
                    //os.writeObject(o);
                    Marshal.writeObject(o, os);

                }
                    break;
                case Constants.CMD_INVOKE: {
                    Long cid = new Long(is.readLong());
                    String className = is.readUTF();
                    long oid = is.readLong();
                    int fxId = is.readShort();
                    Object params[] = null;
                    int pCount = 0xff & is.readByte();
                    if (pCount > 0) {
                        params = new Object[pCount];
                        for (int i = 0; i < pCount; ++i) {
                            params[i] = Marshal.readObject(is);
                        }
                    }

                    //if (client == null)
                    client = (ClientBean) Registry.clientBeans.get(cid);
                    if (client == null) {
                        os.write(Constants.OIS_EXCEPTION);
                        Exception ee = new RemoteException("unknown client id detected - please reconnect");
                        os.writeObject(ee);
                        break;
                    }

                    Object ref = client.objects.get(oid);
                    ISkeleton skel = (ISkeleton) client.get(className);
                    if (skel == null) {
                        String cn = className;
                        byte b[] = OnTheFly.mkskel(cn);
                        Class clazz = CL.cl.loadClass(cn + "_Yang", b);
                        skel = (ISkeleton) clazz.newInstance();

                        cn = className;
                        b = OnTheFly.mkstub(cn);
                        os.write(Constants.OIS_STUB);
                        os.writeUTF(cn + "_Ying");
                        os.writeObject(b);
                        os.flush();
                        clazz = CL.cl.loadClass(cn + "_Ying", b);

                        client.put(className, skel);
                    }

                    // resolve RemoteObject params                
                    // resolveParams(client, params);
                    // NO: use Object readResolve() throws ObjectStreamException
                    // instead

                    Object ret = null;
                    try {
                        if (ref == null) {
                            System.out.println("?");
                        }
                        ret = skel.invoke(ref, fxId, params);
                    } catch (Throwable t) {
                        os.write(Constants.OIS_EXCEPTION);
                        if (!(t instanceof RemoteException)) {
                            t = new RemoteException(t.getMessage(), t);
                        }
                        os.writeObject(t);
                        break;
                    }
                    //sendObject(client, ret);
                    os.writeByte(Constants.OIS_SERIALIZED);
                    Marshal.writeObject(ret, os);
                    //                os.writeObject(ret);
                }
                    break;
                case Constants.CMD_GC: {
                    Long cid = new Long(is.readLong());
                    client = (ClientBean) Registry.clientBeans.get(cid);
                    if (client == null) {
                        os.write(Constants.OIS_EXCEPTION);
                        Exception ee = new RemoteException("unknown client id detected - please reconnect");
                        os.writeObject(ee);
                        break;
                    }

                    int sz = is.readInt();
                    Registry.logFile.writeDate("got gc size: " + sz + " - now: " + (client.objects.size() - sz));
                    while (sz-- > 0) {
                        long oid = is.readLong();
                        // System.out.println("free: " + oid);
                        client.objects.remove(oid);
                    }
                }
                    break;
                case Constants.CMD_TERMINATE: {
                    Long cid = new Long(is.readLong());
                    client = (ClientBean) Registry.clientBeans.get(cid);
                    Registry.logFile.writeDate("client: " + cid + " terminates - freeing: " + client.objects.size());
                    client.clear();
                    Registry.clientBeans.remove(cid);
                    break;
                }
                }
            }
        } catch (Throwable ex) {
        }
        try {
            is.close();
        } catch (IOException e) {
        }
        try {
            os.close();
        } catch (IOException e) {
        }
    }

}