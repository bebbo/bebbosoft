package de.bb.mejb;

import java.security.Identity;
import java.security.Principal;
import java.util.Properties;

import javax.ejb.EJBContext;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;

// Referenced classes of package de.bb.mejb:
//            c, Client, f

class C implements EJBContext {

    private SimpleHomeBean home;

    C(SimpleHomeBean c1) {
        home = c1;
    }

    public Principal getCallerPrincipal() {
        Client client = Client.getClient();

        return client.getPrincipal();
    }

    public EJBHome getEJBHome() {
        return home;
    }

    public EJBLocalHome getEJBLocalHome() {
        return null;
    }

    public boolean getRollbackOnly() throws IllegalStateException {
        Client client = Client.getClient();
        return client.userTransaction.isRollbackOnly();
    }

    public UserTransaction getUserTransaction() throws IllegalStateException {
        Client client = Client.getClient();
        UserTA f1 = client.userTransaction;
        return f1;
    }

    public boolean isCallerInRole(String s) {
        Client client = Client.getClient();
        return client.isCallerInRole(s);
    }

    public void setRollbackOnly() throws IllegalStateException {
        Client client = Client.getClient();
        client.userTransaction.setRollbackOnly();
    }

    public Identity getCallerIdentity() {
        return null;
    }

    public boolean isCallerInRole(Identity identity) {
        return false;
    }

    public Properties getEnvironment() {
        return null;
    }

    @Override
    public TimerService getTimerService() throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }
}
