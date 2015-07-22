package de.bb.bejy.j2ee;

import java.security.Identity;
import java.security.Principal;
import java.util.Map;
import java.util.Properties;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.SessionContext;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;
import javax.xml.rpc.handler.MessageContext;

import de.bb.bejy.ServerThread;

/**
 * SessionCon text wrapper. Locates the current SessionConext vie current thread and delegates the function call.
 * 
 * @author stefan franke
 * 
 */
class SCWrapper implements SessionContext {
    static final SCWrapper INSTANCE = new SCWrapper();

    public static SC getSessionContext() {
        final ThreadContext tc = ThreadContext.currentThreadContext();
        if (tc.sessionContext == null)
            tc.sessionContext = new SC();
        return tc.sessionContext;
    }

    
    public Identity getCallerIdentity() {
        return getSessionContext().getCallerIdentity();
    }

    
    public Principal getCallerPrincipal() {
        return getSessionContext().getCallerPrincipal();
    }

    
    public EJBHome getEJBHome() {
        // TODO Auto-generated method stub
        return null;
    }

    
    public EJBLocalHome getEJBLocalHome() {
        // TODO Auto-generated method stub
        return null;
    }

    
    public Properties getEnvironment() {
        return getSessionContext().getEnvironment();
    }

    
    public boolean getRollbackOnly() throws IllegalStateException {
        return getSessionContext().getRollbackOnly();
    }

    
    public TimerService getTimerService() throws IllegalStateException {
        return getSessionContext().getTimerService();
    }

    
    public UserTransaction getUserTransaction() throws IllegalStateException {
        return getSessionContext().getUserTransaction();
    }

    
    public boolean isCallerInRole(Identity arg0) {
        return getSessionContext().isCallerInRole(arg0);
    }

    
    public boolean isCallerInRole(String arg0) {
        return getSessionContext().isCallerInRole(arg0);
    }

    
    public Object lookup(String arg0) {
        return getSessionContext().lookup(arg0);
    }

    
    public void setRollbackOnly() throws IllegalStateException {
        getSessionContext().setRollbackOnly();
    }

    
    public <T> T getBusinessObject(Class<T> arg0) throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    
    public EJBLocalObject getEJBLocalObject() throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    
    public EJBObject getEJBObject() throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    
    public Class getInvokedBusinessInterface() throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    
    public MessageContext getMessageContext() throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public Map<String, Object> getContextData() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public boolean wasCancelCalled() throws IllegalStateException {
        // TODO Auto-generated method stub
        return false;
    }
}
