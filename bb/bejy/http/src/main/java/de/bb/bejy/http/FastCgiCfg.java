package de.bb.bejy.http;

import de.bb.bejy.Configurable;
import de.bb.bejy.Configurator;

/**
 * the FastCGI host configurator
 * 
 * @author bebbo
 */
public class FastCgiCfg implements Configurator {

	/**
	 * return the name.
	 * 
	 * @return the name
	 * @see de.bb.bejy.Configurator#getName()
	 */
	public String getName() {
		return "FastCGI";
	}

	/**
	 * return the description.
	 * 
	 * @return the description.
	 * @see de.bb.bejy.Configurator#getDescription()
	 */
	public String getDescription() {
		return "a FastCGI host";
	}

	/**
	 * return the path.
	 * 
	 * @return the path.
	 * @see de.bb.bejy.Configurator#getPath()
	 */
	public String getPath() {
		return "fastcgi";
	}

	/**
	 * return null, since there are no dependencies - only the extended class.
	 * 
	 * @return null, since there are no dependencies.
	 * @see de.bb.bejy.Configurator#getRequired()
	 */
	public String getRequired() {
		return null;
	}

	/**
	 * return the extension id.
	 * 
	 * @return the extension id
	 * @see de.bb.bejy.Configurator#getExtensionId()
	 */
	public String getExtensionId() {
		return "de.bb.bejy";
	}

	/**
	 * return the own id.
	 * 
	 * @return the own id.
	 * @see de.bb.bejy.Configurator#getId()
	 */
	public String getId() {
		return "de.bb.bejy.fastcgi";
	}

	/**
	 * return null, since protocols are loaded dynamically.
	 * 
	 * @return null, since protocols are loaded dynamically.
	 * @see de.bb.bejy.Configurator#create()
	 */
	public Configurable create() {
		return new FastCGIHost();
	}

	/**
	 * return false - there is a concrete instance.
	 * 
	 * @return return false - there is a concrete instance.
	 * @see de.bb.bejy.Configurator#loadClass()
	 */
	public boolean loadClass() {
		return false;
	}
}
