package de.bb.jpa;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;
import javax.persistence.PersistenceException;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import com.sun.naming.internal.ResourceManager;

import de.bb.log.Logger;
import de.bb.util.XmlFile;

class PUI implements PersistenceUnitInfo {

	private final static Logger LOG = Logger.getLogger(PUI.class);
	
	private XmlFile xml;
	private String pid;
	private String dataSourceName;
	private PersistenceUnitTransactionType transactionType;

	PUI(String id) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
			for (Enumeration<URL> e = cl.getResources("META-INF/persistence.xml"); e.hasMoreElements();) {
				try {
					URL url = e.nextElement();
					InputStream is = url.openStream();
					XmlFile xml = new XmlFile();
					xml.read(is);
					is.close();

					String pid = xml.getString("/persistence/persistence-unit", "name", null);
					if (id.equals(pid)) {
						this.xml = xml;
						this.pid = pid;

						readXml();

						return;
					}
				} catch (IOException ex) {
				}
			}
		} catch (Exception e) {
			throw new PersistenceException("persistence-unit not loaded: " + id, e);
		}
		throw new PersistenceException("persistence-unit not found: " + id);
	}

	private void readXml() {
		dataSourceName = xml.getContent("/persistence/persistence-unit/jta-data-source");
		if (dataSourceName != null)
			dataSourceName = dataSourceName.trim();

		final String taType = xml.getString("/persistence/persistence-unit", "transaction-type", "jta");
		this.transactionType = PersistenceUnitTransactionType.valueOf(taType);
	}

	public void addTransformer(ClassTransformer arg0) {
		// TODO Auto-generated method stub

	}

	public boolean excludeUnlistedClasses() {
		// TODO Auto-generated method stub
		return false;
	}

	public ClassLoader getClassLoader() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<URL> getJarFileUrls() {
		// TODO Auto-generated method stub
		return null;
	}

	public DataSource getJtaDataSource() {
		try {
			Context context = NamingManager.getInitialContext(getProperties());
			return (DataSource) context.lookup(dataSourceName);
		} catch (Exception ex) {
			LOG.error(ex.getMessage(), ex);
		}
		return null;
	}

	public List<String> getManagedClassNames() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<String> getMappingFileNames() {
		// TODO Auto-generated method stub
		return null;
	}

	public ClassLoader getNewTempClassLoader() {
		// TODO Auto-generated method stub
		return null;
	}

	public DataSource getNonJtaDataSource() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getPersistenceProviderClassName() {
		final String ppcn = xml.getContent("/persistence/persistence-unit/provider");
		if (ppcn != null)
			return ppcn.trim();
		return "de.bb.jpa.PP";
	}

	public String getPersistenceUnitName() {
		return pid;
	}

	public URL getPersistenceUnitRootUrl() {
		// TODO Auto-generated method stub
		return null;
	}

	public Properties getProperties() {
		Properties props = new Properties();
		try {
			@SuppressWarnings("rawtypes")
			final Hashtable ie0 = ResourceManager.getInitialEnvironment(null);
			Hashtable<String, String> ie = ie0;
			for (Entry<String, String> e : ie.entrySet()) {
				props.setProperty(e.getKey(), e.getValue());
			}
		} catch (NamingException e1) {
		}
		for (Enumeration<String> e = xml.getSections("/persistence/persistence-unit/properties/property").elements(); e
				.hasMoreElements();) {
			String key = e.nextElement();
			String name = xml.getString(key, "name", null);
			String value = xml.getString(key, "value", null);
			if (name != null)
				props.put(name, value);
		}
		return props;
	}

	public PersistenceUnitTransactionType getTransactionType() {
		return this.transactionType;
	}

	@Override
	public String getPersistenceXMLSchemaVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ValidationMode getValidationMode() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getDataSourceName() {
		return this.dataSourceName;
	}
}
