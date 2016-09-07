package de.bb.bejy.j2ee;

import java.security.Identity;
import java.security.Principal;
import java.util.HashSet;
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

public class SC implements SessionContext {

    public Identity getCallerIdentity() {
        return null;
    }

    public Principal getCallerPrincipal() {
        return ThreadContext.currentUserPrincipal();
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
        // TODO Auto-generated method stub
        return null;
    }

    public boolean getRollbackOnly() throws IllegalStateException {
        // TODO Auto-generated method stub
        return false;
    }

    public TimerService getTimerService() throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    public UserTransaction getUserTransaction() throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isCallerInRole(Identity arg0) {
        // TODO Auto-generated method stub
        return true;
    }

    public boolean isCallerInRole(String role) {
        return ThreadContext.isCallerInRole(role);
    }

    public boolean isCallerInOneRoleOf(HashSet<String> roles) {
        return ThreadContext.isCallerInOneRoleOf(roles);
    }

    
    public Object lookup(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public void setRollbackOnly() throws IllegalStateException {
        // TODO Auto-generated method stub

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

    
    public Map<String, Object> getContextData() {
        // TODO Auto-generated method stub
        return null;
    }

    
    public boolean wasCancelCalled() throws IllegalStateException {
        // TODO Auto-generated method stub
        return false;
    }

}
