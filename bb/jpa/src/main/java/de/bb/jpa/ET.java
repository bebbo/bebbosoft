package de.bb.jpa;

import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;

class ET implements EntityTransaction {

	private EM em;
	Connection conn;
	private boolean isRollbackOnly;
	private boolean isDead;

	ET(EM em) {
		this.em = em;
	}

	public void begin() {
		if (conn == null)
			this.conn = em.emf.getConnection(this);
		else if (isDead)
			throw new PersistenceException("transaction is (already) inactive");

		// try {
		// conn.isValid(100);
		// } catch (SQLException e) {
		// throw new PersistenceException(e);
		// }
	}

	public void commit() {
		if (isRollbackOnly) {
			throw new PersistenceException("transaction is marked for rollback");
		}
		try {
			isDead = true;
			conn.commit();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			em.emf.releaseConnection(this, conn);
		}
	}

	public boolean getRollbackOnly() {
		return isRollbackOnly;
	}

	public boolean isActive() {
		return !isDead;
	}

	public void rollback() {
		try {
			isDead = true;
			conn.rollback();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			em.emf.releaseConnection(this, conn);
		}
	}

	public void setRollbackOnly() {
		isRollbackOnly = true;
	}

	protected void finalize() {
		isDead = true;
		if (conn != null) {
			em.emf.releaseConnection(this, conn);
		}
	}
}
