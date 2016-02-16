package de.bb.jpa;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;

import de.bb.util.ByteRef;
import de.bb.util.LRUCache;
import de.bb.util.Pair;

class EM implements EntityManager {

	private static final LRUCache<String, Pair<Class<?>, String>> QUERYCACHE = new LRUCache<String, Pair<Class<?>, String>>();
	private static final ByteRef SELECT = new ByteRef("SELECT");
	private static final ByteRef DISTINCT = new ByteRef("DISTINCT");
	private static final ByteRef OBJECT = new ByteRef("OBJECT");
	private static final ByteRef NEW = new ByteRef("NEW");
	private static final ByteRef FROM = new ByteRef("FROM");
	EMF emf;
	private ET currentTa;

	EM(EMF emf, Map argumentMap) {
		this.emf = emf;
	}

	public void clear() {
		// TODO Auto-generated method stub

	}

	public void close() {
		// TODO Auto-generated method stub

	}

	public boolean contains(Object arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public Query createNamedQuery(String queryName) {
		// TODO Auto-generated method stub
		return null;
	}

	public Query createNativeQuery(String nativeQuery) {
		try {
			currentTa.begin();

			PreparedStatement ps = currentTa.conn.prepareStatement(nativeQuery);
			Class<?> clazz = null;
			return new Q(clazz, ps);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage(), e);
		}
	}

	public Query createNativeQuery(String arg0, Class arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public Query createNativeQuery(String arg0, String arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public Query createQuery(final String query) {
		try {
			Pair<Class<?>, String> p = QUERYCACHE.get(query);
			if (p == null) {
				p = translate(query);
				QUERYCACHE.put(query, p);
			}
			currentTa.begin();
			PreparedStatement ps = currentTa.conn.prepareStatement(p.getSecond());
			Query q = new Q(p.getFirst(), ps);
			return q;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Convert the query to SQL
	 * 
	 * @param query
	 * @return
	 */
	private Pair<Class<?>, String> translate(String query) {
		ByteRef br = new ByteRef(query.trim().replace('\n', ' ').replace('\r', ' ').replace('\t', ' '));
		final ByteRef kind = br.nextWord();
		if (SELECT.equalsIgnoreCase(kind))
			return translateSelect(br);

		throw new PersistenceException("not yet implemented");
	}

	private Pair<Class<?>, String> translateSelect(ByteRef br) {
		ByteRef translated = SELECT.append(" ");			
		
		ByteRef word = br.nextWord();

		// DISTINCT is passed
		if (word.equalsIgnoreCase(DISTINCT)) {
			translated = translated.append(DISTINCT).append(" ");
			word = br.nextWord();
		}
		
		do {
			int space = br.indexOf(' ');
			if (space < 0) space = br.length();
			int komma = br.indexOf(',');
			if (komma < 0) komma = br.length();
			
			if (space < komma) {
				
			}
			
		} while (!br.substring(0, 5).clone().nextWord().equalsIgnoreCase(FROM));
		
		
		return null;
	}

	public <T> T find(Class<T> arg0, Object arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public void flush() {
		// TODO Auto-generated method stub

	}

	public Object getDelegate() {
		// TODO Auto-generated method stub
		return null;
	}

	public FlushModeType getFlushMode() {
		// TODO Auto-generated method stub
		return null;
	}

	public <T> T getReference(Class<T> arg0, Object arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public EntityTransaction getTransaction() {
		if (currentTa == null)
			currentTa = new ET(this);
		return currentTa;
	}

	public boolean isOpen() {
		// TODO Auto-generated method stub
		return false;
	}

	public void joinTransaction() {
		// TODO Auto-generated method stub

	}

	public void lock(Object arg0, LockModeType arg1) {
		// TODO Auto-generated method stub

	}

	public <T> T merge(T arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public void persist(Object arg0) {
		// TODO Auto-generated method stub

	}

	public void refresh(Object arg0) {
		// TODO Auto-generated method stub

	}

	public void remove(Object arg0) {
		// TODO Auto-generated method stub

	}

	public void setFlushMode(FlushModeType arg0) {
		// TODO Auto-generated method stub

	}

	public <T> EntityGraph<T> createEntityGraph(Class<T> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public EntityGraph<?> createEntityGraph(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public <T> TypedQuery<T> createNamedQuery(String arg0, Class<T> arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public StoredProcedureQuery createNamedStoredProcedureQuery(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Query createQuery(CriteriaUpdate arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Query createQuery(CriteriaDelete arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public <T> TypedQuery<T> createQuery(String arg0, Class<T> arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public StoredProcedureQuery createStoredProcedureQuery(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public StoredProcedureQuery createStoredProcedureQuery(String arg0, Class... arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public StoredProcedureQuery createStoredProcedureQuery(String arg0, String... arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public void detach(Object arg0) {
		// TODO Auto-generated method stub

	}

	public <T> T find(Class<T> arg0, Object arg1, Map<String, Object> arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2, Map<String, Object> arg3) {
		// TODO Auto-generated method stub
		return null;
	}

	public CriteriaBuilder getCriteriaBuilder() {
		// TODO Auto-generated method stub
		return null;
	}

	public EntityGraph<?> getEntityGraph(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return emf;
	}

	public LockModeType getLockMode(Object arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Metamodel getMetamodel() {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String, Object> getProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isJoinedToTransaction() {
		// TODO Auto-generated method stub
		return false;
	}

	public void lock(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
		// TODO Auto-generated method stub

	}

	public void refresh(Object arg0, Map<String, Object> arg1) {
		// TODO Auto-generated method stub

	}

	public void refresh(Object arg0, LockModeType arg1) {
		// TODO Auto-generated method stub

	}

	public void refresh(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
		// TODO Auto-generated method stub

	}

	public void setProperty(String arg0, Object arg1) {
		// TODO Auto-generated method stub

	}

	public <T> T unwrap(Class<T> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
