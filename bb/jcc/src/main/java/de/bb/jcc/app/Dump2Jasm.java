package de.bb.jcc.app;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

/* 
 * Created on 04.04.2005
 * written by Stefan Bebbo Franke
 * (c) by BebboSoft 2004
 */

/**
 * Converts a Java dump file into a jasm file.
 * @author sfranke
 */
public class Dump2Jasm
{

  public static void main(String[] args)
  {
    
    if (args.length == 0 || args.length > 2 || "-?".equals(args[0]))
    {
      System.out.println("USAGE: Dump2Jasm <dumpFile> [<jasmFile>]");
      //return;
      args = new String[]{"M3.dump"};
    }
    String dumpFileName = args[0];
    int lDot = dumpFileName.lastIndexOf('.');
    String jasmFileName = dumpFileName.substring(0, lDot + 1) + "jasm";
    if (args.length == 2)
    {
      jasmFileName = args[1];
    }
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(dumpFileName)));
      PrintStream ps = new PrintStream(new FileOutputStream(jasmFileName));

      String line = br.readLine();
      if (!line.startsWith("Compiled"))      
        unexpected(line);       
      
      ps.println("; converted from" + line.substring(8) + "\r\n");
      
      // class def
      line = br.readLine();
      ps.println(line);

      boolean openMethod = false;      
      for(;;)
      {
        line = br.readLine();
        if (line == null)
          break;
        
        if (line.length() == 0) {
          if (openMethod) {
            openMethod = false;
            ps.println("  }");
          }
          continue;
        }
        
        if (line.charAt(0) != ' ' && line.endsWith(";")) {
          openMethod = true;
          line = line.substring(0, line.length() - 1);
          ps.println("  " + line);
          ps.println("  {");
          continue;
        }
        
        if (line.trim().startsWith("Signature"))
          continue;
        if (line.trim().startsWith("Code:"))
          continue;
        
        int num = line.indexOf('#');
        int com = line.indexOf("//");
        if (num > 0 && com > 0 && com > num) {
          com = line.indexOf(' ', com);
          line = line.substring(0, num) + line.substring(com);
        }
        ps.println(line);
      }
      
      
      ps.close();
    } catch (IOException e) {
      e.printStackTrace();
    }  
  }

  /**
   * @param line
   * @throws IOException
   */
  private static void unexpected(String line) throws IOException
  {
    throw new IOException("unexpected: " + line);
  }
}
