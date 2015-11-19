package de.bb.rmi;

/**
 * @author bebbo
 */
class LongHash
{
  private final static int primes[] =
    {
      53,
      97,
      193,
      389,
      769,
      1543,
      3079,
      6151,
      12289,
      24593,
      49157,
      98317,
      196613,
      393241,
      786433,
      1572869,
      3145739,
      6291469,
      12582917,
      25165843,
      50331653,
      100663319,
      201326611,
      402653189,
      805306457,
      1610612741 };
  private Entry[] table;
  private int threshold;
  private int count;
  private int primeIndex;

  /**
   * Creates a new object with an initial size for 32 elements
   */
  public LongHash()
  {
    reset();
  }

  private synchronized void reset()
  {
    table = new Entry[primes[0]];
    primeIndex = 0;
    threshold = 32;
    count = 0;  
  
  }
  
  public void clear()
  {
    reset();
  }
  /**
   * Returns the number of keys in this hashtable.
   *
   * @return  the number of keys in this hashtable.
   */
  public int size()
  {
    return count;
  }

  /**
  * Tests if this hashtable maps no keys to values.
   *
   * @return  <code>true</code> if this hashtable maps no keys to values;
   *          <code>false</code> otherwise.
   */
  public boolean isEmpty()
  {
    return count == 0;
  }
  /**
   * Returns the value to which the specified key is mapped in this hashtable.
   *
   * @param   key   a key in the hashtable.
   * @return  the value to which the key is mapped in this hashtable;
   *          <code>null</code> if the key is not mapped to any value in
   *          this hashtable.
   * @see     #put(Object, Object)
   */
  public synchronized Object get(long key)
  {
    Entry t[] = table;
    int i = ((int) key & 0x7FFFFFFF) % t.length;
    for (Entry e = t[i]; e != null; e = e.next)
    {
      if (e.key == key)
        return e.val;
    }
    return null;
  }

  /**
   * Increases the capacity of and internally reorganizes this 
   * hashtable, in order to accommodate and access its entries more 
   * efficiently.  This method is called automatically when the 
   * number of keys in the hashtable exceeds this hashtable's capacity 
   * and load factor. 
   */
  private void rehash()
  {
    int oldCapacity = table.length;
    Entry oldTable[] = table;

    threshold += threshold;
    int newCapacity = primes[++primeIndex];

    Entry t[] = new Entry[newCapacity];
    table = t;

    for (int i = oldCapacity; i-- > 0;)
    {
      for (Entry f, e = oldTable[i]; e != null; e = f)
      {
        f = e.next;
        int j = ((int) e.key & 0x7FFFFFFF) % newCapacity;
        e.next = t[j];
        t[j] = e;
      }
    }
  }
  /**
   * Maps the specified <code>key</code> to the specified 
   * <code>value</code> in this hashtable. Neither the key nor the 
   * value can be <code>null</code>. <p>
   *
   * The value can be retrieved by calling the <code>get</code> method 
   * with a key that is equal to the original key. 
   *
   * @param      key     the hashtable key.
   * @param      value   the value.
   * @return     the previous value of the specified key in this hashtable,
   *             or <code>null</code> if it did not have one.
   * @see     #get(Object)
   */
  public synchronized Object put(long key, Object value)
  {
    // Make sure the value is not null
    if (value == null)
    {
      return null;
    }

    Entry t[] = table;
    int i = ((int) key & 0x7FFFFFFF) % t.length;
    // search key
    for (Entry e = t[i]; e != null; e = e.next)
    {
      if (e.key == key)
      {
        Object o = e.val;
        e.val = value;
        return o;
      }
    }

    Entry e = new Entry();
    e.key = key;
    e.val = value;
    e.next = t[i];
    t[i] = e;

    if (count++ >= threshold)
    {
      // Rehash the table if the threshold is exceeded
      rehash();
    }
    return null;
  }
  /**
   * Removes the key (and its corresponding value) from this 
   * hashtable. This method does nothing if the key is not in the hashtable.
   *
   * @param   key   the key that needs to be removed.
   * @return  the value to which the key had been mapped in this hashtable,
   *          or <code>null</code> if the key did not have a mapping.
   */
  public synchronized Object remove(long key)
  {
    Entry t[] = table;
    int i = ((int) key & 0x7FFFFFFF) % t.length;
    // search key
    for (Entry f = null, e = t[i]; e != null; e = e.next)
    {
      if (e.key == key)
      {
        --count;
        if (f == null)
          t[i] = e.next;
        else
          f.next = e.next;
        return e.val;
      }
      f = e;
    }
    return null;
  }
  
  /**
   * 
   * @author bebbo
   *
   */
  private static class Entry
  {
    long key;
    Object val;
    Entry next;
  }

  /*  
    public static void main(String args[])
    {
      LongHash lh = new LongHash();
      
      lh.put(1, "1");
      lh.put(54, "54");
      lh.put(107, "107");
      System.out.println("" + lh.get(1));
      System.out.println("" + lh.get(54));
      System.out.println("" + lh.get(107));
      lh.remove(54);
      System.out.println("" + lh.get(1));
      System.out.println("" + lh.get(54));
      System.out.println("" + lh.get(107));
      lh.remove(1);
      System.out.println("" + lh.get(1));
      System.out.println("" + lh.get(54));
      System.out.println("" + lh.get(107));
      lh.remove(107);
      System.out.println("" + lh.get(1));
      System.out.println("" + lh.get(54));
      System.out.println("" + lh.get(107));
    }
   */
}
