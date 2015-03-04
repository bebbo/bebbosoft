package de.bb.bejy.j2ee;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

public class UT implements UserTransaction {

    
    public void begin() throws NotSupportedException, SystemException {
        // TODO Auto-generated method stub
        
    }

    
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
            SecurityException, IllegalStateException, SystemException {
        // TODO Auto-generated method stub
        
    }

    
    public int getStatus() throws SystemException {
        // TODO Auto-generated method stub
        return 0;
    }

    
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        // TODO Auto-generated method stub
        
    }

    
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        // TODO Auto-generated method stub
        
    }

    
    public void setTransactionTimeout(int arg0) throws SystemException {
        // TODO Auto-generated method stub
        
    }

}
