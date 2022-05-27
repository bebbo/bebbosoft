/**
 * written by Stefan Bebbo Franke
 * (c) 1999-2004 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved
 * all rights reserved
 *
 * BebboSoft Lexer.
 */
package de.bb.bex2;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import de.bb.bex2.xml.NsInfo;
/**
 * @author bebbo
 */
public class TagEntry
{
  /** parent reference. */
  private TagEntry parent;
  /** reference to start and end definition. */
  private ParseEntry start, end;
  /** children of this tag. */
  private ArrayList<TagEntry> children;
  /** the tag name. */
  private String name;
  /** all known prefixes. Is null, if no own prefixes exist. */
  private HashMap<String, NsInfo> prefixes;
  private NsInfo nsi;
  /**
   * Create a TagEntry.
   * 
   * @param start
   */
  public TagEntry(ParseEntry start)
  {
    this.start = start;
    if (start != null)
      start.setTag(this);
  }
  /**
   * Append an entry as last child.
   * 
   * @param entry
   * @return
   */
  public TagEntry append(TagEntry entry)
  {
    if (children == null)
      children = new ArrayList<TagEntry>();
    children.add(entry);
    entry.setParent(this);
    return entry;
  }
  /**
   * Set the parent entry.
   * 
   * @param entry
   */
  public void setParent(TagEntry entry)
  {
    this.parent = entry;
  }
  /**
   * set the end ParseEntry.
   * 
   * @param end
   *          the end entry.
   * 
   * @author bebbo
   */
  public void setEnd(ParseEntry end)
  {
    this.end = end;
  }
  /**
   * Return the tag name.
   * This might return no reasonable values for synthetic tag entries.
   * E.g. the implicit root.
   * @return the tag name.
   */
  public String getName()
  {
    if (name == null)
    {
      ParseEntry child = start.getChild(0); 
      if (child != null)
        name = child.getText();      
    }
    return name;
  }
  /**
   * Return the parent tag.
   * @return the parent tag
   */
  public TagEntry getParent()
  {
    return parent;
  }
  /**
   * Return the prefixes.
   * @return Returns the prefixes.
   */
  public HashMap<String, NsInfo> getPrefixes()
  {
    return prefixes;
  }
  /**
   * add a namespace prefix to the current node.
   * @param prefix used prefix
   * @param uri used uri
   * @param nsi retrieved namespace info
   */
  public void addPrefix(String prefix, String uri, NsInfo nsi)
  {
    if (prefixes == null) {
      prefixes = new HashMap<String, NsInfo>();
    }
    prefixes.put(prefix, nsi);
  }
  /**
   * Retrieve the namespace information for a given prefix.
   * 
   * @param prefix
   * @return NamespaceInfo or null
   */
  public NsInfo getNamespaceInfo(String prefix)
  {
    if (prefixes != null) {
      Object o = prefixes.get(prefix);
      if (o != null) {
        return (NsInfo) o;
      }
    }
    if (parent != null)
      return parent.getNamespaceInfo(prefix);
    return null;
  }
  /**
   * Retrieve the used uri given prefix.
   * 
   * @param prefix
   * @return NamespaceInfo or null
   */
  public String getNamespaceURI(String prefix)
  {
    NsInfo nsi = getNamespaceInfo(prefix);
    if (nsi == null)
      return null;
    return nsi.getURI();
  }
  /**
   * Apply the namespace attribute to all children.
   */
  public void applyNamespace()
  {
    if (children == null)
      return;
    if (prefixes == null)
      return;
    for (Iterator<TagEntry> i = children.iterator(); i.hasNext();) {
      TagEntry te = i.next();
      if (te.prefixes == null) {
        te.prefixes = prefixes;
      } else {
        te.prefixes.putAll(prefixes);
      }
    }
  }
  /**
   * Apply the namespace attributes to the specified ParseEntry.
   * @param te the tageEntry which namespaces are applied to this tag.
   */
  void applyNamespace(TagEntry te)
  {
    if (prefixes == null)
      return;
    if (te.prefixes == null)
      te.prefixes = prefixes;
    else
      te.prefixes.putAll(prefixes);
  }
  /**
   *  (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString()
  {
    return start.toString() + end;
  }
  /**
   * Return the start ParseEntry.
   * @return the start ParseEntry.
   */
  public ParseEntry getStart()
  {
    return start;
  }
  /**
   * Return the end ParseEntry. Can be the same as the start ParseEntry.
   * @return the end ParseEntry.
   */
  public ParseEntry getEnd()
  {
    if (end == null) end = start;
    return end;
  }
  /**
   * remove the last child. used to drop tags with errors.
   */
  void dropLast()
  {
    children.remove(children.size() - 1);
  }
  /**
   * Store a namespace information.
   * @param currentNsi
   */
  public void setNsInfo(NsInfo currentNsi)
  {
    this.nsi = currentNsi;
  }
  
  /**
   * Return the NsInfo for this tag.
   * @return the NsInfo for this tag.
   */
  public NsInfo getNsInfo()
  {
    return nsi;
  }
  /**
   * Return an ArrayList containing all child tags.
   * @return An ArrayList containing all child tags.
   */
  public ArrayList<TagEntry> getChildren()
  {
    return children;
  }
 }