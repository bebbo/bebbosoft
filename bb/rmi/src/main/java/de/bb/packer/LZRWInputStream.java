package de.bb.packer;

import java.io.IOException;
import java.io.InputStream;
;

/**
 */
public class LZRWInputStream extends InputStream
{
  private InputStream in;
  private byte[] inData;
  private byte[] outData;
//  private int offsetHighByteMask;
  private int offsetHighByteShift;
  private int lenMask;
  private int outPos;
  private int outEnd;
//  private int inPos;
//  private int bufferSize;
  private byte[] history;
  private int historyPos;
  private int cmdPos;
  private int commandBits;
  private int command;
  private int rPos;
  private int compressedLength;

  public LZRWInputStream(InputStream in) throws IOException
  {
    this.in = in;
    int lenBits = 0xff & in.read();
    int bufferSize = readLength();
    
    int offsetBits = 16 - lenBits;

    int maxOffset = (1 << offsetBits) + 1;    
    
    offsetHighByteShift = lenBits;
    lenMask = (1 << lenBits) - 1;

    int inSize = bufferSize + (bufferSize + 15) / 16;    

//    int offsetBits = 16 - lenBits;
//    maxOffset = (1 << offsetBits) + 2;
    
    inData = new byte[inSize];
    history = new byte[maxOffset];
    outData = new byte[bufferSize];
  }
  /**
   * Method readLength.
   * @return int
   */
  private int readLength() throws IOException
  {    
    int i, r = 0, s = 0;
    do 
    {
      i = in.read();
      r |= (i&0x7f) << s;
      s += 7;
    } while (i > 127);
    return r;
  }



  void decompress() throws IOException
  {
    if (rPos == cmdPos)
      collect();

//    System.out.println("\r\n*** decompressing");

    int hPos = historyPos;
    int modulo = history.length;
    int wPos = 0;

//    while (wPos < origLength)
    while (rPos < cmdPos)
    {      
      if (--commandBits <= 0)
      {
        command = inData[--cmdPos] & 0xff;
        command |= (inData[--cmdPos] & 0xff) << 8;
        commandBits = 16;
//        DEBUGCMD(command);
      } else
      {
        command &= 0xffff;
      }
      
      command <<= 1;
      
      // copy string
      if (command > 65535)
      {
        int offset = 0xff & inData[--cmdPos];
        int offhi = inData[--cmdPos] & 0xff;
        
        offset |= (offhi >> offsetHighByteShift) << 8;
        
        int len = (offhi & lenMask) + 3;
        int cpo = (hPos - offset - 1 + modulo) % modulo;
        
//        System.out.print("\r\nin(" + offset + "," + len + " : " + hPos + ")");
        while (len-- > 0)
        {
//          DEBUGOUT(history[cpo]);
          history[hPos] = outData[wPos++] = history[cpo];
          cpo = (cpo + 1) % modulo;
          hPos = (hPos + 1) % modulo;
        }
//        System.out.println();
      } else
      {
        // copy byte
//        DEBUGOUT(inData[rPos]);
        history[hPos] = outData[wPos++] = inData[rPos++];
        hPos = (hPos + 1) % modulo;
      }      
    }
    
    historyPos = hPos;
    outPos = 0;
    outEnd = wPos;
//    System.out.println("received : " + compressedLength  + " -> "+ wPos);
  }
  /**
   * Method collect.
   */
  private void collect() throws IOException
  {
    compressedLength = readLength();

    if (compressedLength < 0)
      throw new IOException("EOS");    
            
//    System.out.println("received cprLen: " + compressedLength);
            
    for (int pos = 0; pos < compressedLength;) 
    { 
      int l = in.read(inData, pos, compressedLength - pos);
      pos += l;     
    } 

    cmdPos = compressedLength;
    rPos = 0;
    commandBits = 0;
    command = 0;
  }


  /**
   * @see java.io.InputStream#available()
   */
  public int available() throws IOException
  {
    if (outEnd - outPos > 0)
      return outEnd - outPos;
    if (in.available() > 0)
      decompress();
    return outEnd - outPos;
  }

  /**
   * @see java.io.InputStream#mark(int)
   */
  public synchronized void mark(int readlimit) 
  {
    throw new RuntimeException("mark not supported");
  }

  /**
   * @see java.io.InputStream#read()
   */
  public int read() throws IOException
  {
    if (outPos >= outEnd)
      decompress();
    if (outPos < outEnd)
      return 0xff & outData[outPos++];
    return -1;
  }

  /**
   * @see java.io.InputStream#read(byte, int, int)
   */
  public int read(byte[] b, int off, int len) throws IOException
  {
    if (outPos >= outEnd)
      decompress();
    if (outPos >= outEnd)
      return 0;
    if (outEnd - outPos < len)
      len = outEnd - outPos;
    System.arraycopy(outData, outPos, b, off, len);
    outPos += len;
    return len;
  }

  /**
   * @see java.io.InputStream#read(byte)
   */
  public int read(byte[] b) throws IOException
  {
    return read(b, 0, b.length);
  }

  /**
   * @see java.io.InputStream#reset()
   */
  public synchronized void reset()
  {
    throw new RuntimeException("reset not supported");
  }

  /**
   * @see java.io.InputStream#skip(long)
   */
  public long skip(long n) throws IOException
  {
    if (n > outEnd - outPos)
      n = outEnd - outPos;
    outPos += (int)n;
    return n;
  }

  /**
   * Method DEBUGOUT.
   * @param b
   */
  public static void DEBUGOUT(byte b)
  {
    int ch = b & 0xff;
    if (ch >= 32 && ch <= 127) {
      System.out.print((char)ch);
      if (ch == '\\')
        System.out.print((char)ch);
      return;
    }
    String hex = Integer.toHexString(ch);
    if (hex.length() < 2)
      hex = "0" + hex;
    System.out.print("\\" + hex);    
  }

  /**
   * Method DEBUGCMD.
   * @param command
   */
  public static void DEBUGCMD(int command)
  {
    System.out.print("\r\n");
    for (int i = 0; i < 16; ++i)
    {
      command <<= 1;
      System.out.print(command > 65535 ? "r" : "c");
      command &= 0xffff;
    }
  }

}
