/*****************************************************************************
 * Copyright (c) by Stefan Bebbo Franke 1999-2015.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *****************************************************************************/
package de.bb.bejy.http;

import de.bb.bejy.Configurable;
import de.bb.bejy.Configurator;
import de.bb.util.LogFile;

/**
 * @author bebbo
 */
public class MimeTypeCfg extends Configurable implements Configurator
{
  private final static String PROPERTIES[][] =
    {{"extension", "file extension"},
     {"type", "the associated mime-type"}
    }
  ;
  
  public MimeTypeCfg()
  {
    init("mime type", PROPERTIES);
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getName()
   */
  public String getName()
  {
    String ext = getProperty("extension");
    if (ext == null)
      return "mime type";
    return ext;
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getDescription()
   */
  public String getDescription()
  {
    return "which assigns the content type to a file extension";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getPath()
   */
  public String getPath()
  {
    return "mime-type";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getId()
   */
  public String getId()
  {
    return "de.bb.bejy.http.mime-type";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getExtensionId()
   */
  public String getExtensionId()
  {
    return "de.bb.bejy.http.mime-types";
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#getRequired()
   */
  public String getRequired()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#create()
   */
  public de.bb.bejy.Configurable create()
  {
    return new MimeTypeCfg();
  }
  /* (non-Javadoc)
   * @see de.bb.bejy.Configurator#loadClass()
   */
  public boolean loadClass()
  {
    return false;
  }
  /* (non-Javadoc)
   * @see de.bb.bejy.Configurable#update(de.bb.util.LogFile)
   */
  public void update(LogFile logFile) throws Exception
  {
    getParent().update(logFile);
  }

}
