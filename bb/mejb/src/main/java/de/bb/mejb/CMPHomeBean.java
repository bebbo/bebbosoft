package de.bb.mejb;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public abstract class CMPHomeBean extends SimpleHomeBean implements SimpleHome {
    protected static Object NOPARAM[] = new Object[0];
    private String beanName;

    protected CMPHomeBean(String beanName) {
        this.beanName = beanName;
    }

    protected CMPDbi initDbi(String packageName, String beanName) throws RemoteException {
        try {
            String dbType = Config.getDBType(packageName);
            String s3 = packageName + '.' + dbType + '.' + beanName + "Dbi";
            Logger.info("initializing dbi class " + s3);
            ClassLoader classloader = getClass().getClassLoader();
            Class class1 = classloader.loadClass(s3);
            CMPDbi cmpdbi = (CMPDbi) class1.newInstance();
            cmpdbi.Code(Config.c(packageName));
            return cmpdbi;
        } catch (Exception exception) {
            throw new RemoteException("error while initializing dbi: " + exception.getMessage(), exception);
        }
    }

    protected void remove(CMPBean cmpbean, CMPDbi cmpdbi) throws RemoteException {
        Connection connection = null;
        Client client = Client.getClient();
        try {
            client.a(cmpdbi, beanName);
            connection = cmpdbi.getConnection(client.getPrincipal());
            cmpdbi.remove(connection, cmpbean.getId());
            cmpdbi.c(beanName);
            Config.dblog(client.getPrincipal().getName(), beanName, "delete", cmpbean.toLog());
        } catch (Exception exception) {
            throw new RemoteException("error while deleting object", exception);
        } finally {
            if (connection != null)
                try {
                    cmpdbi.releaseConnection(client.getPrincipal(), connection);
                } catch (Exception exception2) {
                    throw new RemoteException("error while releasing sql connection", exception2);
                }
        }
        return;
    }

    protected void store(CMPBean cmpbean, int i, CMPDbi cmpdbi) throws RemoteException {
        try {
            Client client = Client.getClient();
            client.a(cmpdbi, beanName);
            Connection connection = null;
            PreparedStatement preparedstatement = null;
            try {
                connection = cmpdbi.getConnection(client.getPrincipal());
                String s = cmpbean.getId();
                if (s == null) {
                    preparedstatement = cmpdbi.insert(connection);
                } else {
                    preparedstatement = cmpdbi.update(connection);
                    preparedstatement.setString(i, s);
                }
                cmpbean.writeValues(preparedstatement);
                preparedstatement.executeUpdate();
                if (s == null)
                    cmpbean.setId(cmpdbi.getId(preparedstatement));
                cmpdbi.c(beanName);
                Config.dblog(client.getPrincipal().getName(), beanName, "store", cmpbean.toLog());
            } catch (SQLException sqlexception1) {
                throw sqlexception1;
            } catch (Exception exception1) {
                throw new SQLException(exception1.getMessage());
            } finally {
                if (preparedstatement != null)
                    preparedstatement.close();
                if (connection != null)
                    cmpdbi.releaseConnection(client.getPrincipal(), connection);
            }
        } catch (SQLException sqlexception) {
            throw new RemoteException("error while storing object", sqlexception);
        } catch (Exception exception) {
            throw new RemoteException(exception.getMessage(), exception);
        }
    }

    private CMPBean Code(Object obj, CMPBean cmpbean, CMPDbi cmpdbi) throws RemoteException {
        try {

            long l;
            String s;
            Connection connection;
            ResultSet resultset;
            if (obj == null || "".equals(obj))
                return null;
            l = System.currentTimeMillis();
            s = beanName + '\001' + obj;
            CMPBean cmpbean1 = (CMPBean) cmpdbi.lruCache.get(s);
            if (cmpbean1 != null) {
                cmpbean1.assign(cmpbean, this);
                return cmpbean;
            }
            connection = null;
            resultset = null;
            Object obj1;
            Client client = Client.getClient();
            try {

                connection = cmpdbi.getConnection(client.getPrincipal());
                resultset = cmpdbi.select(connection, obj);
                if (resultset.next()) {
                    cmpbean.readValues(resultset);
                    cmpdbi.d(l, s, cmpbean);
                    return cmpbean;
                }
                return null;
            } finally {
                if (resultset != null) {
                    resultset.close();
                    resultset.getStatement().close();
                }
                if (connection != null)
                    cmpdbi.releaseConnection(client.getPrincipal(), connection);
            }
        } catch (Exception sqlexception) {
            throw new RemoteException("error while reading object by primary key", sqlexception);
        }
    }

    protected CMPBean readByPrimaryKey(Object obj, CMPDbi cmpdbi) throws RemoteException {
        CMPBean cmpbean = (CMPBean) internCreate();
        cmpbean.setEntityContext(new EC(this, cmpbean));
        Code(obj, cmpbean, cmpdbi);
        if (cmpbean.getId() == null)
            return null;
        else
            return cmpbean;
    }

    protected CMPBean readByPrimaryKey(Object obj, ResultSet resultset) throws RemoteException {
        CMPBean cmpbean = (CMPBean) internCreate();
        try {
            cmpbean.readValues(resultset);
        } catch (Exception exception) {
            throw new RemoteException("error while reading object by primary key", exception);
        }
        return cmpbean;
    }

    protected void load(CMPBean cmpbean, CMPDbi cmpdbi) throws RemoteException {
        Code(cmpbean.getId(), cmpbean, cmpdbi);
    }

    protected Collection queryCollection(String s, Object aobj[]) throws RemoteException {
        return fetchCollection(getDbi(), s, aobj, 0, 0x7fffffff);
    }

    protected Collection queryCollection(String s, Object aobj[], int i, int j) throws RemoteException {
        return fetchCollection(getDbi(), s, aobj, i, j);
    }

    private Collection fetchCollection(CMPDbi cmpdbi, String s, Object aobj[], int i, int j) throws RemoteException {
        try {
            long l;
            LinkedList linkedlist;
            l = System.currentTimeMillis();
            linkedlist = new LinkedList();
            String s1;
            Exception exception;
            Collection collection1;
            try {
                s1 = c(s, aobj) + ":" + i + "-" + j;
                Collection collection = (Collection) cmpdbi.lruCache.get(s1);
                if (collection != null) {
                    collection1 = copyCachedResult(collection);
                    return collection1;
                }
            } finally {
                long l1 = System.currentTimeMillis() - l;
                Logger.debug("execution time: " + l1 + ", query: " + s);
            }
            Connection connection = null;
            PreparedStatement preparedstatement = null;
            Client client = Client.getClient();
            try {
                connection = cmpdbi.getConnection(client.getPrincipal());
                preparedstatement = connection.prepareStatement(s);
                for (int k = 0; k < aobj.length; k++)
                    preparedstatement.setObject(k + 1, aobj[k]);

                ResultSet resultset = preparedstatement.executeQuery();
                if (i > 0)
                    resultset.absolute(i);
                while (resultset.next() && j-- > 0) {
                    String s2 = resultset.getString(1);
                    CMPBean cmpbean = readByPrimaryKey(s2, resultset);
                    if (cmpbean != null)
                        linkedlist.add(cmpbean);
                }
                resultset.close();
                cmpdbi.d(l, s1, linkedlist);
            } catch (NullPointerException _ex) {
            } catch (SQLException sqlexception) {
                if (preparedstatement != null)
                    preparedstatement.close();
                preparedstatement = null;
                if (connection != null)
                    connection.close();
                throw sqlexception;
            } finally {
                if (preparedstatement != null)
                    preparedstatement.close();
                if (connection != null)
                    cmpdbi.releaseConnection(client.getPrincipal(), connection);
            }
            return copyCachedResult(linkedlist);
        } catch (Exception exception1) {
            throw new RemoteException("error while copying collection", exception1);
        }
    }

    private Collection copyCachedResult(Collection collection) throws Exception {
        Logger.debug("cache hit");
        ArrayList linkedlist = new ArrayList(collection.size());
        CMPBean cmpbean;
        for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
            cmpbean = (CMPBean) iterator.next();
            cmpbean = cmpbean.cloneWith(this);
            linkedlist.add(cmpbean);
        }

        return linkedlist;
    }

    private String c(String s, Object aobj[]) {
        StringBuffer stringbuffer = new StringBuffer(s);
        for (int i = 0; i < aobj.length; i++) {
            stringbuffer.append('\001');
            stringbuffer.append(aobj[i]);
        }

        return stringbuffer.toString();
    }

    protected double queryDouble(String s, Object aobj[]) throws RemoteException {
        CMPDbi cmpdbi;
        String s1;
        double d;
        long l;
        cmpdbi = getDbi();
        s1 = c(s, aobj);
        Double double1 = (Double) cmpdbi.lruCache.get(s1);
        if (double1 != null)
            return double1.doubleValue();
        d = 0.0D;
        l = System.currentTimeMillis();
        Connection connection = null;
        PreparedStatement preparedstatement = null;
        Client client = Client.getClient();

        try {
            connection = cmpdbi.getConnection(client.getPrincipal());
            preparedstatement = connection.prepareStatement(s);
            for (int i = 0; i < aobj.length; i++)
                preparedstatement.setObject(i + 1, aobj[i]);

            ResultSet resultset = preparedstatement.executeQuery();
            if (resultset.next()) {
                d = resultset.getDouble(1);
                cmpdbi.d(l, s1, new Double(d));
            }
            resultset.close();
        } catch (Exception ex) {
            throw new RemoteException(ex.getMessage());
        } finally {
            if (preparedstatement != null)
                try {
                    preparedstatement.close();
                } catch (SQLException e) {
                }
            if (connection != null)
                cmpdbi.releaseConnection(client.getPrincipal(), connection);
        }
        return d;
    }

    protected CMPDbi getDbi() {
        return null;
    }
}
