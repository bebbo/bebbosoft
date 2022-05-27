/******************************************************************************
 * $Source: /export/CVS/java/de/bb/jsp/src/main/java/de/bb/jsp/CodeStream.java,v $
 * $Revision: 1.4 $
 * $Date: 2004/01/09 19:37:37 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 * 
 ******************************************************************************
    NON COMMERCIAL PUBLIC LICENSE
 ******************************************************************************

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:

    1. Every product and solution using this software, must be free
      of any charge. If the software is used by a client part, the
      server part must also be free and vice versa.

    2. Each redistribution must retain the copyright notice, and
      this list of conditions and the following disclaimer.

    3. Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in
      the documentation and/or other materials provided with the
      distribution.

    4. All advertising materials mentioning features or use of this
      software must display the following acknowledgment:
        "This product includes software developed by BebboSoft,
          written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

    5. Redistributions of any form whatsoever must retain the following
      acknowledgment:
        "This product includes software developed by BebboSoft,
          written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"

 ******************************************************************************
  DISCLAIMER OF WARRANTY

  Software is provided "AS IS," without a warranty of any kind.
  You may use it on your own risk.

 ******************************************************************************
  LIMITATION OF LIABILITY

  I SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY YOU OR ANY THIRD PARTY
  AS A RESULT OF USING OR DISTRIBUTING SOFTWARE. IN NO EVENT WILL I BE LIABLE
  FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
  CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS
  OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE
  SOFTWARE, EVEN IF I HAVE ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

 *****************************************************************************
  COPYRIGHT

  (c) 2003 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

  Created on 23.10.2003

 *****************************************************************************/

package de.bb.jsp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;


/**
* Helper class to generate properly indented code
*/
class CodeStream extends ByteArrayOutputStream
{
  private final JspCC jspCC;
  private int indent;

  Vector lines = new Vector();
  Vector file = new Vector();

  void indent()
  {
    indent += 2;
  }
  void unindent()
  {
    if (indent > 0)
      indent -= 2;
  }
  void write(String s) throws IOException
  {
    write(JspCC.spaces, 0, (indent - 2) % 64 + 2);
    write(s.getBytes());
  }
  void writeln(String s) throws IOException
  {
    write(s);
    lf();
  }
  void lf() throws IOException
  {
    write(0xd);
    write(0xa);
    lines.add(new Integer(this.jspCC.line));
    file.add(this.jspCC.currentFile);
  }

  void addLineInfo()
  {
    lines.add(new Integer(this.jspCC.line));
    file.add(null); // no real file -> no file name
  }

  void addFullInfo()
  {
    lines.add(new Integer(this.jspCC.line));
    file.add(this.jspCC.currentFile);
  }

  void writeln2(String s) throws IOException
  {
    write(JspCC.spaces, 0, 4);
    write(s.getBytes());
    lf();
  }
  void writeln3(String s) throws IOException
  {
    int curLine = this.jspCC.line;
    this.jspCC.line = this.jspCC.codeEndLine;
    writeln2(s);
    this.jspCC.line = curLine;
  }
  CodeStream(JspCC jspCC)
  {
    this(jspCC, 0);
  }
  CodeStream(JspCC jspCC, int i)
  {
    indent = i;
    this.jspCC = jspCC;
  }

  /**
   * @see java.io.ByteArrayOutputStream#reset()
   */
  public synchronized void reset()
  {
    super.reset();
    lines.clear();
  }

  /**
   * Search the Java line number by JSP line number.
   * @param jspLine the JSP line number
   * @return Integer with the Java line number or null
   */
  Integer searchJavaLine(int offset, int jspLine)
  {
    for (Enumeration e = lines.elements(), f = file.elements();
      e.hasMoreElements();
      )
    {
      Integer l = (Integer) e.nextElement();
      Object o = f.nextElement();
      if (o == this.jspCC.currentFile && jspLine == l.intValue())
        return new Integer(offset);
      if (o != null)
        ++offset;
    }
    return null;
  }

  /**
   * Search the JSP line number by Java line number.
   * @param jspLine the JSP line number
   * @return Integer with the Java line number or null
   */
  Integer searchJspLine(int offset, int javaLine)
  {
    int idx = javaLine - offset;
    int i = 0;
    while (i < idx)
    {
      if (i >= file.size())
        return null;
      if (file.get(i) == null)
        ++idx;
      ++i;        
    }
    return (Integer) lines.get(i);
  }

  /**
   * Search the JSP line number by Java line number.
   * @param jspLine the JSP line number
   * @return Integer with the Java line number or null
   */
  String searchJspFileName(int offset, int javaLine)
  {
    int idx = javaLine - offset;
    int i = 0;
    while (i < idx)
    {      if (i >= file.size())
        return null;
      if (file.get(i) == null)
        ++idx;
      ++i;
      
    }
    while (file.get(i) == null && i+1 < file.size())
    {
      ++i;
    }
    return (String) file.get(i);
  }

  void writeSmap(int javaLine, StringBuffer sb, HashMap incFiles)
  { 
    for (int i = 0, sz = lines.size(); i < sz; ++i)
    {
      String fileName = (String)file.elementAt(i);
      if (fileName == null)
      {
        continue;
      }
      Object fileId = incFiles.get(fileName);
      
      int startJavaLine = ++javaLine;
      int startJspLine = ((Integer)lines.elementAt(i)).intValue();
      int j = i + 1;
      String fileName2 = null;
      for(; j < sz; ++j)
      {
        fileName2 = (String)file.elementAt(j);
        if (fileName2 == null)
          break;
        if (fileName2 != fileName)
        {
          --j;
          break;
        }
        ++javaLine;
      }
      if (j >= lines.size())
      {
        j = lines.size() - 1;
      }
      int endJspLine = ((Integer)lines.elementAt(j)).intValue();
      
      if (fileName2 != null)
      {
        sb.append(startJspLine);
        sb.append('#');
        sb.append(fileId);
        sb.append(',');
        sb.append(endJspLine - startJspLine + 1);
        sb.append(':');
        sb.append(startJavaLine);
        sb.append(",1\r\n");
      } else {
        if (j > i + 1)
        {
          int end2 = ((Integer)lines.elementAt(j - 2)).intValue();
          sb.append(startJspLine);
          sb.append('#');
          sb.append(fileId);
          sb.append(',');
          sb.append(end2 - startJspLine + 1);
          sb.append(':');
          sb.append(startJavaLine);
          sb.append(",1\r\n");
          
          startJspLine = ((Integer)lines.elementAt(j - 1)).intValue();
          startJavaLine = javaLine;          
        }
        sb.append(startJspLine);
        sb.append('#');
        sb.append(fileId);
        sb.append(',');
        sb.append(endJspLine - startJspLine + 1);
        sb.append(':');
        sb.append(startJavaLine);
        sb.append(",0\r\n");        
      }
            
      i = j;
    }
  }

  /**
   * Write the SMAP information for this code stream to the bos.
   * @param javaLine Java line number where this code starts.
   * @param bos stream to write SMAP info to
   * @param incFiles map to get the file number.
   * /
  void writeSmap(int javaLine, ByteArrayOutputStream bos, HashMap incFiles) throws IOException
  {
    // System.out.println(lines);
    Enumeration e = lines.elements(), f = file.elements();
    if (!e.hasMoreElements())
      return;
    Integer l = (Integer) e.nextElement();
    Object o = f.nextElement();
    Integer fid2 = null, fid = o == null ? null : (Integer)incFiles.get(o);
    
    int javaCount = 0;
    int parallel = 0; // count when both increments are 1  
    
    while(e.hasMoreElements())
    {
      Integer l2 = (Integer) e.nextElement();
      Object o2 = f.nextElement();
      fid2 = o2 == null ? null : (Integer)incFiles.get(o2);

      // same file? check possible compression
      if (fid == fid2)
      {
        // same jsp line??
        if (l.equals(l2) && parallel == 0) {
          ++javaCount;
          ++javaLine;
          continue;
        }
        
        // 
        if (l2.intValue() - l.intValue() == 1 + parallel && javaCount == 0)
        {
          ++parallel;
          ++javaLine;
          continue;
        }
      }
      
      if (fid != null)
      {
        if (fid2 == null && l2.intValue() - l.intValue() > 1 && l2.intValue() - l.intValue() + javaCount > 1)
        {
          int lines = l2.intValue() - l.intValue() + javaCount;
          bos.write((l.intValue()+ "#" + fid + "," + lines + ":" + (javaLine - javaCount) + "," + (parallel > 0 ? 1 : 0) +"\r\n").getBytes());
        } else
        if (javaCount > 0)
        {
          bos.write((l.intValue() + "#" + fid + ":" + (javaLine-javaCount) + "," + javaCount + "\r\n").getBytes());
        } else
        if (parallel > 0)
        {
          bos.write((l.intValue() + "#" + fid + "," + (parallel+1) + ":" + (javaLine-parallel) + "\r\n").getBytes());
        } else 
        {
          bos.write((l.intValue() + "#" + fid + ":" + javaLine + "\r\n").getBytes());
        }
      }
      parallel = 0;
      javaCount = 0;
      if (o2 != null)
      {  
        ++javaLine; 
      } 

      fid = fid2;        
      l = l2;
      o = o2;
    }
  }
  /**/
}
/******************************************************************************
 * $Log: CodeStream.java,v $
 * Revision 1.4  2004/01/09 19:37:37  bebbo
 * @B fixed NPE
 *
 * Revision 1.3  2004/01/03 18:51:46  bebbo
 * @R rewrote the SMAP generation
 *
 * Revision 1.2  2003/12/18 10:43:56  bebbo
 * @N new method to add more line information
 *
 * Revision 1.1  2003/10/23 20:35:34  bebbo
 * @R moved JspCC inner classes into separate files
 * @R jspCC is now reusable
 * @N added more caching to enhance reusability
 *
 *****************************************************************************/