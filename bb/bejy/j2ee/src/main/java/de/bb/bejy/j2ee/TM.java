package de.bb.bejy.j2ee;

import java.util.HashMap;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

public class TM implements TransactionManager{

	private boolean rollbackOnly;
	private Transaction ta;
	private int count;
	private HashMap<String, Object> tmBeanMap = new HashMap<>();


	@Override
	public void begin() throws NotSupportedException, SystemException {
		if (ta == null)
			ta = new TA(this);
		++count;
	}

	@Override
	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {
		--count;
		if (count != 0)
			return;
		
		
		// TODO: real commit
	}

	@Override
	public int getStatus() throws SystemException {
		return rollbackOnly ? -1 : 0;
	}

	@Override
	public Transaction getTransaction() throws SystemException {
		return ta;
	}


	@Override
	public void rollback() throws IllegalStateException, SecurityException, SystemException {
		setRollbackOnly();
	}

	@Override
	public void setRollbackOnly() throws IllegalStateException, SystemException {
		this.rollbackOnly = true;
	}

	@Override
	public void setTransactionTimeout(int arg0) throws SystemException {
		
	}

	@Override
	public void resume(Transaction arg0) throws InvalidTransactionException, IllegalStateException, SystemException {
	}
	@Override
	public Transaction suspend() throws SystemException {
		return ta;
	}

	
	public static TM getTM() {
		ThreadContext tc = ThreadContext.currentThreadContext();
		return tc.getCurrentTM();
	}

	public Object getBean(String className) {
		return tmBeanMap.get(className);
	}

	public void putBean(String className, Object bean) {
		tmBeanMap.put(className, bean);
	}

}
