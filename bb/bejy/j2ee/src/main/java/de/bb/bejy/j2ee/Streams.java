package de.bb.bejy.j2ee;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @author bebbo
 */
class Streams
{
  Socket socket;
  OIS is;
  ObjectOutputStream os;
  int useCount = 0;

  Streams(Socket s) throws IOException
  {
    socket = s;
    
    OutputStream _os = s.getOutputStream();
//    _os = new GZIPOutputStream(_os);
//    _os = new ZipOutputStream(_os);
//    _os = new LZRWOutputStream(_os);
    _os = new BufferedOutputStream(_os);
    os = new ObjectOutputStream(_os);
    os.flush(); // important!!! or application will block!!

    InputStream _is = s.getInputStream();
//  _is = new ZipInputStream(_is);
//    _is = new GZIPInputStream(_is);
//    _is = new LZRWInputStream(_is);
    _is = new BufferedInputStream(_is);
    is = new OIS(_is);
  }
  /**
   * Method close.
   */
  public void release()
  {
    try
    {
      socket.close();
    } catch (Exception e)
    {
    }
    try
    {
      is.close();
    } catch (Exception e)
    {
    }
    try
    {
      os.close();
    } catch (Exception e)
    {
    }
  }
}
