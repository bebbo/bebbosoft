package de.bb.packer;

class Compressor
{
  int lenHighByteMask;
  int lenHighByteShift;
  int lenMax;

  int windowMask;
  int maxOffset;
  int hashTable[];
  int linkedList[];

  /**
   * Creates a Compress object which does some kind of LZRW compression. 16 bits
   * are used to store the length of some data and the offset to a previous
   * occurance of that data. The command words are stored separated from the
   * data stream.
   * @param lenBits how many bits are used to store a length.
   */
  Compressor(int lenBits)
  {
    if (lenBits < 1 || lenBits > 10) 
      lenBits = 8;
    lenHighByteShift = lenBits - 8;
    int offsetBits = 16 - lenBits;
    lenMax = 1 << lenBits;
    lenHighByteMask = 255 << offsetBits & 0xff;

    windowMask = (1 << offsetBits) - 1;
    maxOffset = (1 << offsetBits) + 2;
    int windowSize = lenMax * 2 + maxOffset;
    hashTable = new int[256];
    linkedList = new int[windowSize];
    lenMax--;
  }

  byte[] compress(byte inData[])
  {
    byte outData[] = new byte[inData.length + 8];
    // copy original length
    outData[0] = (byte) inData.length;
    outData[1] = (byte) (inData.length >> 8);
    outData[2] = (byte) (inData.length >> 16);
    outData[3] = (byte) (inData.length >> 24);
    
    // setup compressor
    int outLen = 8;
    int maxLen = inData.length - 2;
    int l = maxLen;
    int command = 1;
    for (int j1 = 0; j1 < 256; j1++)
      hashTable[j1] = -1;

    for (int i = 0; i < inData.length && maxLen > outLen; i++)
    {
      // last 2 bytes are raw written.
      if (i >= inData.length - 2)
      {
        outData[outLen++] = inData[i];
        // add a 0 -> copy a byte
        command <<= 1;
      } else
      {
        int hash = 0xff & (inData[i] ^ inData[i + 1] ^ inData[i + 2]);
        if (hashTable[hash] < 0)
        {
          outData[outLen++] = inData[i];
          hashTable[hash] = i;
          linkedList[i & lenMax] = -1;
          command <<= 1;
        } else
        {         
          int maxLenFound = -1;
          int bestPosition = -1;
          for (int offset = hashTable[hash]; offset >= 0 && i <= offset + lenMax; offset = linkedList[offset & lenMax])
          {
            int j3;
            for (j3 = 1; j3 < maxOffset && i + j3 < inData.length; j3++)
              if (inData[i + j3] != inData[offset + j3])
                break;

            if (j3 <= maxLenFound)
              continue;
            maxLenFound = j3;
            bestPosition = offset;
            if (maxLenFound == maxOffset)
              break;
          }

          if (maxLenFound < 3)
          {
            outData[outLen++] = inData[i];
            linkedList[i & lenMax] = hashTable[hash];
            hashTable[hash] = i;
            command <<= 1;
          } else
          {
            int offset = i - bestPosition;
            outData[--maxLen] = (byte) offset;
            outData[--maxLen] = (byte) (offset >> lenHighByteShift & lenHighByteMask | maxLenFound - 3);
            // update hash for the copied entry and skip those bytes
            while (maxLenFound-- > 0)
            {
              if (i < inData.length - 2)
              {
                int i2 = 0xff & (inData[i] ^ inData[i + 1] ^ inData[i + 2]);
                linkedList[i & lenMax] = hashTable[i2];
                hashTable[i2] = i;
              }
              i++;
            }
            i--;
            
            // add a 1 -> copy string
            command <<= 1;
            command |= 1;
          }
        }
      }
      // emit command word
      if (command > 65535)
      {
        outData[l + 1] = (byte) command;
        outData[l] = (byte) (command >> 8);
        l = maxLen -= 2;
        command = 1;
      }
    }

    // left align command and append it
    if (command != 1)
    {
      for (; command < 0x10000; command <<= 1);
      outData[l + 1] = (byte) command;
      outData[l] = (byte) (command >> 8);
    }
    
    // if compression was NOT successfull
    if (maxLen <= outLen)
    {
      outData[4] = (byte) inData.length;
      outData[5] = (byte) (inData.length >> 8);
      outData[6] = (byte) (inData.length >> 16);
      outData[7] = (byte) (inData.length >> 24);
      System.arraycopy(inData, 0, outData, 8, inData.length);
    } else
    {
      // copy compressed data
      int totalLen = (outLen + inData.length) - maxLen;
      byte outBuffer[] = new byte[totalLen + 8];
      System.arraycopy(outData, 0, outBuffer, 0, outLen);
      outBuffer[4] = (byte) totalLen;
      outBuffer[5] = (byte) (totalLen >> 8);
      outBuffer[6] = (byte) (totalLen >> 16);
      outBuffer[7] = (byte) (totalLen >> 24);
      System.arraycopy(outData, maxLen, outBuffer, outLen, inData.length - maxLen);
      outData = outBuffer;
    }
    return outData;
  }

  static int inLength(byte compressedHeader[])
  {
    return compressedHeader[0] & 0xff | (compressedHeader[1] & 0xff)
      << 8 | (compressedHeader[2] & 0xff)
      << 16 | (compressedHeader[3] & 0xff)
      << 24;
  }

  static int outLength(byte compressedHeader[])
  {
    return compressedHeader[4] & 0xff | (compressedHeader[5] & 0xff)
      << 8 | (compressedHeader[6] & 0xff)
      << 16 | (compressedHeader[7] & 0xff)
      << 24;
  }

  byte[] decompress(byte inData[])
  {
    int origLength = inLength(inData);
    int compressedLength = outLength(inData);
    byte outData[] = new byte[origLength];
    if (compressedLength == origLength)
    {
      System.arraycopy(inData, 8, outData, 0, compressedLength);
      return outData;
    }
    int cmdPos = compressedLength;
    int i1 = 8;
    int outPos = 0;
    int commandBits = 16;
    int command = inData[--cmdPos] & 0xff;
    command |= (inData[--cmdPos] & 0xff) << 8;
    while (outPos < origLength)
    {
      command <<= 1;
      
      // copy string
      if (command > 65535)
      {
        int offset = 0xff & inData[--cmdPos];
        offset |= (lenHighByteMask & inData[--cmdPos]) << lenHighByteShift;
        for (int len = (inData[cmdPos] & windowMask) + 3; len-- > 0;)
        {
          outData[outPos] = outData[outPos - offset];
          outPos++;
        }
      } else
      {
        // copy byte
        outData[outPos++] = inData[i1++];
      }
      
      if (--commandBits <= 0)
      {
        command = inData[--cmdPos] & 0xff;
        command |= (inData[--cmdPos] & 0xff) << 8;
        commandBits = 16;
      } else
      {
        command &= 0xffff;
      }
    }
    return outData;
  }

}
