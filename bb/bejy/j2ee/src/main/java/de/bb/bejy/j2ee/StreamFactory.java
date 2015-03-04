package de.bb.bejy.j2ee;

import java.net.Socket;

import de.bb.util.Pool;

/**
 * @author bebbo
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
class StreamFactory implements Pool.Factory
{
  private String ip;
  private int port;
  StreamFactory(String ip, int port)
  {
    this.ip = ip;
    this.port = port;
  }

  /**
   * @see de.bb.util.Pool.Factory#create()
   */
  public Object create() throws Exception
  {
    Socket s = new Socket(ip, port);
    Streams ss = new Streams(s);
    return ss;
  }

  /**
   * @see de.bb.util.Pool.Factory#destroy(java.lang.Object)
   */
  public void destroy(Object o)
  {
    Streams ss = (Streams)o;
    ss.release();
  }

  /**
   * @see de.bb.util.Pool.Factory#isIdle(java.lang.Object)
   */
  public boolean isIdle(Object o)
  {
    return false;
  }

  /**
   * @see de.bb.util.Pool.Factory#validate(java.lang.Object)
   */
  public boolean validate(Object o)
  {
    Streams ss = (Streams)o;
    return ss.socket.isConnected();
  }

  /**
   * @see de.bb.util.Pool.Factory#validateKey(java.lang.Object)
   */
  public boolean validateKey(Object key)
  {
    return true;
  }

}
