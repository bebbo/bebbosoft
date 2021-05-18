package de.bb.bejy.j2ee;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

class TA implements Transaction {

	private TM tm;

	TA(TM tm) {
		this.tm = tm;
	}

	@Override
	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {
		tm.commit();
	}

	@Override
	public boolean delistResource(XAResource arg0, int arg1) throws IllegalStateException, SystemException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean enlistResource(XAResource arg0) throws RollbackException, IllegalStateException, SystemException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getStatus() throws SystemException {
		return tm.getStatus();
	}

	@Override
	public void registerSynchronization(Synchronization arg0)
			throws RollbackException, IllegalStateException, SystemException {
		// TODO Auto-generated method stub

	}

	@Override
	public void rollback() throws IllegalStateException, SystemException {
		tm.rollback();
	}

	@Override
	public void setRollbackOnly() throws IllegalStateException, SystemException {
		tm.setRollbackOnly();
	}


}
