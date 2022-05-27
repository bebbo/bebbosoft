package de.bb.jpa;

import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

public class PP implements PersistenceProvider {

	private final static Map<String, String> cast(Map m) {
		return m;
	}

	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo pui, Map argumentMap) {
		return new EMF(pui, cast(argumentMap));
	}

	public EntityManagerFactory createEntityManagerFactory(String id, Map argumentMap) {
		PUI pui = new PUI(id);
		if (!getClass().getName().equals(pui.getPersistenceProviderClassName()))
			throw new PersistenceException("wrong persistence provider: " + pui.getPersistenceProviderClassName()
					+ "\r\nexpected: " + getClass().getName());
		if (argumentMap == null)
			argumentMap = pui.getProperties();
		else
			argumentMap.putAll(pui.getProperties());
		return new EMF(pui, argumentMap);
	}

	@Override
	public void generateSchema(PersistenceUnitInfo arg0, Map arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean generateSchema(String arg0, Map arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ProviderUtil getProviderUtil() {
		// TODO Auto-generated method stub
		return null;
	}
}
