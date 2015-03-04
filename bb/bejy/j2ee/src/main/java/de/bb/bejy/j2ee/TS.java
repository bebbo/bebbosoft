package de.bb.bejy.j2ee;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

import javax.ejb.EJBException;
import javax.ejb.Timer;
import javax.ejb.TimerService;

class TS implements TimerService{

    
    public Timer createTimer(long arg0, Serializable arg1) throws IllegalArgumentException, IllegalStateException,
            EJBException {
        // TODO Auto-generated method stub
        return null;
    }

    
    public Timer createTimer(Date arg0, Serializable arg1) throws IllegalArgumentException, IllegalStateException,
            EJBException {
        // TODO Auto-generated method stub
        return null;
    }

    
    public Timer createTimer(long arg0, long arg1, Serializable arg2) throws IllegalArgumentException,
            IllegalStateException, EJBException {
        // TODO Auto-generated method stub
        return null;
    }

    
    public Timer createTimer(Date arg0, long arg1, Serializable arg2) throws IllegalArgumentException,
            IllegalStateException, EJBException {
        // TODO Auto-generated method stub
        return null;
    }

    
    public Collection getTimers() throws IllegalStateException, EJBException {
        // TODO Auto-generated method stub
        return null;
    }

}
