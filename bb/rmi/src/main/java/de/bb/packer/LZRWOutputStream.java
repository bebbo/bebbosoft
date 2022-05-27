package de.bb.packer;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This implementation adds a LZRW1-A compression to the underlying stream.
 * To enhance compression, an internal buffer is used, so writes are delayed
 * until buffer overflows or are forced by flush().
 */
public class LZRWOutputStream extends OutputStream
{
//  private int lenHighByteMask;
  private int lenHighByteShift;
  //  private int lenMax;
  private int maxOffset;
  private int hashTable[] = new int[256];
  private int linkedList[];

  private byte inData[], outData[], history[];
  private byte b1[] = new byte[1];
  private OutputStream out;

  // positions in ring buffer
  private int inStop;

  // statistic
  private long bytesIn, bytesOut;
  
  private int maxLen;
  private int histPos;
  private int bufferSize;

  public LZRWOutputStream(OutputStream out) throws IOException
  {
    this(out, 6, 0x1000);
  }
  public LZRWOutputStream(OutputStream out, int lenBits) throws IOException
  {
    this(out, lenBits, 0);
  }
  public LZRWOutputStream(OutputStream out, int lenBits, int bufferSize)
    throws IOException
  {
    this.out = new BufferedOutputStream(out, 1412);
    if (lenBits < 2 || lenBits > 8)
      lenBits = 6;
      
    int offsetBits = 16 - lenBits;

    maxLen = (1 << lenBits) + 2;
    maxOffset = (1 << offsetBits) + 1;

    lenHighByteShift = lenBits;

    linkedList = new int[maxOffset];
    history = new byte[maxOffset];

    // min / max  for bufferSize
    if (bufferSize < maxOffset)
      bufferSize = maxOffset;
    bufferSize &= 0x1fffffff;
    this.bufferSize = bufferSize;
      
    inData = new byte[bufferSize + 3]; // to enable hash for [i+1], [i+2]
    
    int outSize = bufferSize + (bufferSize + 15) / 16;    
    outData = new byte[outSize];

    // init hashtable
    for (int j1 = 0; j1 < 256; j1++)
      hashTable[j1] = -1;

    // transmit setting
    this.out.write(lenBits);
    bytesOut = 1 + sendLength(bufferSize);
    this.out.flush();
  }

  /**
   * Method sendLength.
   * @param length
   */
  private int sendLength(int length) throws IOException
  {
    int r = 1;
    while (length > 0x7f)
    {
      out.write( length&0x7f | 0x80);
      length >>>= 7;
      ++r;
    }
    out.write(length);
    return r;
  }

  /**
   * Perform the compression using a LZRW1-A compressor.
   * @throws IOException
   */
  void compress() throws IOException
  {
    if (inStop == 0)
      return;
//    System.out.println("\r\n*** compressing");
    int command = 1;
    int outPos = 0;
    int cmdPos = outData.length;
    int copyInfoPos = cmdPos - 2;
    
    int hash = 0xff & (inData[0] ^ inData[1] ^ inData[(2)]);
    for (int i = 0; i < inStop;)
    {
      command <<= 1;

      int maxLenFound = 0;
      int bestPosition = -1;
      int lastOffset = -1;
      
      int inLen = inStop - i;
      int cLen = maxLen > inLen ? inLen : maxLen;

      for (int position = hashTable[hash];
        position >= 0;
        position = linkedList[position])
      {
        int hLen = (histPos - position + maxOffset) % maxOffset;
        if (hLen == 0)
          continue;
        if (hLen <= lastOffset)
          break;
        lastOffset = hLen;

        int strLen = strcmp(position, i, hLen, cLen);
        if (strLen > maxLenFound)
        {
          maxLenFound = strLen;
          bestPosition = position;
          if (strLen >= cLen)
            break;
        }
      }

      if (maxLenFound < 3)
      {
        // copy byte
        outData[outPos++] = inData[i]; 
      } else
      { 
        // add a 1 -> copy string
        command |= 1;
        
        // copy offset and length
        int offset = (histPos - bestPosition - 1 + maxOffset) % maxOffset;
        outData[copyInfoPos - 1] = (byte) offset;
        outData[copyInfoPos -= 2] =
          (byte) (((offset >>8) << lenHighByteShift)  | (maxLenFound - 3));
        // update hash for the copied entry and skip those bytes
//        System.out.print("\r\nout(" + (offset+1) + "," + maxLenFound + ":" + histPos + ")");
        while (--maxLenFound > 0)
        {
          linkedList[histPos] = hashTable[hash];
          hashTable[hash] = histPos;
          hash ^= history[histPos] = inData[i];
          histPos = (histPos + 1) % maxOffset;
          ++i;

          hash = 0xff & (hash ^ inData[i + 2]);
        }
      } 

      linkedList[histPos] = hashTable[hash];
      hashTable[hash] = histPos;
      hash ^= history[histPos] = inData[i]; 
      histPos = (histPos + 1) % maxOffset;
      ++i;
      hash = 0xff & (hash ^ inData[i + 2]);


      // emit command word, if full
      if (command > 65535)
      {
        outData[cmdPos - 1] = (byte) command;
        outData[cmdPos -= 2] = (byte) (command >> 8);
        command = 1;
        if (copyInfoPos < cmdPos)
        {
          cmdPos = copyInfoPos;
        }
        copyInfoPos = cmdPos - 2;
      }
    }
    
    // left align command and append it,
    if (command != 1)
    {
      while ( command < 0x10000)
      {
        command <<= 1;
      }      
      outData[cmdPos - 1] = (byte) command;
      outData[cmdPos -= 2] = (byte) (command >> 8);
      if (copyInfoPos < cmdPos)
      {
        cmdPos = copyInfoPos;
      }
    }

    // compressed Length
    int cprLen = outPos + outData.length - cmdPos;
//    System.out.println("send: " + inStop + " -> " + cprLen);
    sendLength(cprLen);

    // update stats
    bytesIn += inStop;
    bytesOut += cprLen + 1;

    // write compressed Data
    out.write(outData, 0, outPos);
    // write commands
    out.write(outData, cmdPos, outData.length - cmdPos);
    
    inStop = 0;
  }

  /**
   * Compare the string starting at position k in history 
   * with the string at position i in inData, 
   * if string exceeds history length (hLen), then continue comparation with
   * inData, until compare length (cLen) is reached.
   *  
   * @param k position in history (the ring buffer for already compressed data)
   * @param i position in inData (the input buffer)
   * @param hLen max length of comparation in history, if hLen is exceeded,
   * comparation is done with not yet compressed data, starting at i in inData.
   * @param cLen max length of total comparation.
   * @return the count of identical bytes, up to cLen.
   */
  private int strcmp(int k, int i, int hLen, int cLen)
  {
    if (hLen > cLen) hLen = cLen;
        
    int j = i;
    int stop = i + hLen;
    for (; j != stop; ++j, k = (k + 1) % maxOffset)
    {
      if (inData[j] != history[k]) 
        break;
    }
    if (j != stop)
      return j - i;

    stop = i + cLen;
    for (k = i; j != stop; ++j, ++k)
    {
      if (inData[j] != inData[k]) 
        break;
    }
    return j - i;
  }

  /**
   * @see java.io.OutputStream#flush()
   */
  public void flush() throws IOException
  {
    compress();
    out.flush();
  }

  /**
   * @see java.io.OutputStream#write(byte, int, int)
   */
  public void write(byte[] b, int off, int len) throws IOException
  {
    while (len > 0)
    {
      int windowSize = bufferSize - inStop;
      if (windowSize > len)
        windowSize = len;
        
      System.arraycopy(b, off, inData, inStop, windowSize);
      off += windowSize;
      len -= windowSize;
      inStop += windowSize;
//      System.out.print("\r\nwindowsize=" + windowSize);
      if (inStop == bufferSize)
        compress();
    }
  }

  /**
   * @see java.io.OutputStream#write(byte)
   */
  public void write(byte[] b) throws IOException
  {
    write(b, 0, b.length);
  }

  /**
   * @see java.io.OutputStream#write(int)
   */
  public void write(int b) throws IOException
  {
    b1[0] = (byte) b;
    write(b1, 0, 1);
  }
  /**
   * @see java.io.OutputStream#close()
   */
  public void close() throws IOException
  {
    out.close();
  }

  public String toString()
  {
    return "LZRWOutputStream - data length="
      + bytesIn
      + " compressed length="
      + bytesOut
      + " ratio="
      + (bytesOut * 100 / bytesIn);
  }
}
