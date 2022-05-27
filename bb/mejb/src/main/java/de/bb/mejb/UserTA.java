package de.bb.mejb;

import de.bb.util.Pool;
import java.io.PrintStream;
import java.security.Principal;
import java.sql.Connection;
import java.util.*;
import javax.transaction.*;

class UserTA implements UserTransaction {
    private HashSet inTA;
    private HashMap a;
    private boolean isMarkedForRollback;
    private boolean isActive;
    private Principal principal;

    UserTA(Principal principal) {
        inTA = new HashSet();
        a = new HashMap();
        this.principal = principal;
    }

    public boolean isRollbackOnly() {
        return isMarkedForRollback;
    }

    public synchronized void setRollbackOnly() {
        if (!isActive)
            throw new IllegalStateException("no UserTransaction in use");
        isMarkedForRollback = true;
    }

    public synchronized void begin() throws SystemException {
        if (isActive) {
            throw new IllegalStateException("UserTransaction is in use");
        } else {
            isActive = true;
            isMarkedForRollback = false;
            inTA = new HashSet();
            a = new HashMap();
            Logger.debug("begin transaction");
            return;
        }
    }

    public synchronized void commit() throws SystemException, RollbackException {
        boolean flag;
        if (!isActive)
            throw new IllegalStateException("this UserTransaction is not active");
        if (isMarkedForRollback) {
            rollback();
            throw new RollbackException("TA was rolled back!");
        }
        flag = false;
        try {
            for (Iterator iterator = inTA.iterator(); iterator.hasNext();) {
                Pool pool = (Pool) iterator.next();
                Connection connection = null;
                try {
                    connection = (Connection) pool.obtain(principal);
                    connection.commit();
                    connection.setAutoCommit(true);
                } catch (Exception _ex) {
                    try {
                        pool.renew(principal);
                    } catch (Exception _ex2) {
                    }
                    flag = true;
                }
                try {
                    if (connection != null) {
                        pool.unlock(principal);
                        pool.release(principal, connection);
                        pool.release(principal, connection);
                    }
                } catch (Exception _ex) {
                    flag = true;
                }
            }

        } finally {
            isActive = false;
        }
        if (flag)
            throw new SystemException("FATAL exception in commit!!!");
        else
            return;
    }

    public int getStatus() {
        return 0;
    }

    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        boolean flag;
        if (!isActive)
            throw new IllegalStateException("this UserTransaction is not active");
        flag = false;
        try {
            for (Iterator iterator = inTA.iterator(); iterator.hasNext();) {
                Pool pool = (Pool) iterator.next();
                Connection connection = null;
                try {
                    connection = (Connection) pool.obtain(principal);
                    connection.rollback();
                    connection.setAutoCommit(true);
                } catch (Exception _ex) {
                    try {
                        pool.renew(principal);
                    } catch (Exception _ex2) {
                    }
                    flag = true;
                }
                try {
                    if (connection != null) {
                        pool.unlock(principal);
                        pool.release(principal, connection);
                        pool.release(principal, connection);
                    }
                } catch (Exception _ex) {
                    flag = true;
                }
            }

        } finally {
            for (Iterator iterator1 = a.entrySet().iterator(); iterator1.hasNext();) {
                java.util.Map.Entry entry = (java.util.Map.Entry) iterator1.next();
                CMPDbi cmpdbi = (CMPDbi) entry.getKey();
                HashSet hashset = (HashSet) a.get(cmpdbi);
                String s;
                for (Iterator iterator2 = hashset.iterator(); iterator2.hasNext(); cmpdbi.c(s))
                    s = (String) iterator2.next();

            }

            isActive = false;
        }
        if (flag)
            throw new SystemException("FATAL exception in commit!!!");
        else
            return;
    }

    public void setTransactionTimeout(int i) {
    }

    void Code() {
        System.out.println("closing a timeouted session");
        if (isActive)
            try {
                rollback();
            } catch (SystemException _ex) {
            }
    }

    public void addDbi(CMPDbi cmpdbi, String s) throws Exception {
        if (!isActive)
            return;
        Pool pool = cmpdbi.a;
        if (inTA.add(pool)) {
            Connection connection = (Connection) pool.obtain(principal);
            pool.lock(principal);
            connection.setAutoCommit(false);
        }
        HashSet hashset = (HashSet) a.get(cmpdbi);
        if (hashset == null) {
            hashset = new HashSet();
            a.put(cmpdbi, hashset);
        }
        hashset.add(s);
    }
}
