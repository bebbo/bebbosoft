package de.bb.jpa;

import java.sql.Connection;
import java.util.Collections;
import java.util.Map;

import javax.persistence.Cache;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import de.bb.util.Pool;

class EMF implements EntityManagerFactory {

	private boolean isOpen = true;

	PUI pui;

	private Pool pool;

	private DataSource dataSource;

	EMF(final PersistenceUnitInfo ppui, Map<String, String> argumentMap) {
		this.pui = (PUI) ppui;

		final String dsName = pui.getDataSourceName();
		if (dsName != null) {
			if (pui.getTransactionType().equals(PersistenceUnitTransactionType.JTA)) {
				this.dataSource = pui.getJtaDataSource();
			} else {
				this.dataSource = pui.getNonJtaDataSource();
			}
		}
		if (dataSource == null) {
			String jdbcDriver = argumentMap.get("jdbc.driver");
			String jdbcUrl = argumentMap.get("jdbc.url");
			String user = argumentMap.get("jdbc.user");
			String password = argumentMap.get("jdbc.password");
			try {
				this.pool = new de.bb.util.Pool(new de.bb.sql.JdbcFactory(jdbcDriver, jdbcUrl, user, password));
			} catch (Exception e) {
				throw new PersistenceException(e);
			}
		}
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		isOpen = false;

		if (pool != null)
			pool.setMaxCount(0);
	}

	@Override
	public EntityManager createEntityManager() {
		return new EM(this, Collections.EMPTY_MAP);
	}

	@Override
	public EntityManager createEntityManager(Map argumentMap) {
		return new EM(this, argumentMap);
	}

	@Override
	public boolean isOpen() {
		return isOpen;
	}

	Connection getConnection(Object key) {
		try {
			if (pool != null)
				return (Connection) pool.obtain(key);

			return dataSource.getConnection();
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	void releaseConnection(Object key, Connection conn) {
		if (pool != null)
			pool.release(key, conn);
	}

	@Override
	public <T> void addNamedEntityGraph(String arg0, EntityGraph<T> arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addNamedQuery(String arg0, Query arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public EntityManager createEntityManager(SynchronizationType arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EntityManager createEntityManager(SynchronizationType arg0, Map arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Cache getCache() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CriteriaBuilder getCriteriaBuilder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Metamodel getMetamodel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PersistenceUnitUtil getPersistenceUnitUtil() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T unwrap(Class<T> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
