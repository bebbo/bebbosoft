package de.bb.mejb;

import de.bb.rmi.EasyRemoteObject;
import java.rmi.RemoteException;
import java.sql.*;
import javax.ejb.EntityBean;

public abstract class CMPBean extends EasyRemoteObject implements EntityBean, Simple {

    protected CMPBean(CMPHomeBean cmphomebean) throws RemoteException {
        myHome = cmphomebean;
    }

    protected abstract void readValues(ResultSet resultset) throws SQLException;

    protected abstract void writeValues(PreparedStatement preparedstatement) throws SQLException;

    public abstract String getId() throws RemoteException;

    protected abstract void setId(String s) throws RemoteException;

    public abstract StringBuffer toLog() throws RemoteException;

    protected abstract void assign(CMPBean cmpbean, CMPHomeBean cmphomebean) throws RemoteException;

    protected CMPBean cloneWith(CMPHomeBean cmphomebean) throws RemoteException {
        CMPBean cmpbean = (CMPBean) cmphomebean.internCreate();
        assign(cmpbean, cmphomebean);
        cmpbean.setEntityContext(new EC(cmphomebean, cmpbean));
        return cmpbean;
    }

    protected String encodeXML(Object obj) {
        if (obj == null) {
            return null;
        } else {
            String s = obj.toString();
            s = s.replaceAll("<", "&lt;");
            s = s.replaceAll(">", "&gt;");
            s = s.replaceAll("&", "&amp;");
            return s;
        }
    }

    protected boolean encodeXML(boolean flag) {
        return flag;
    }

    protected int encodeXML(int i) {
        return i;
    }

    protected long encodeXML(long l) {
        return l;
    }

    protected double encodeXML(double d) {
        return d;
    }

    protected CMPHomeBean myHome;
}
