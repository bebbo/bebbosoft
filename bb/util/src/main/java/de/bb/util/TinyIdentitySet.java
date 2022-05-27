package de.bb.util;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.AbstractSet;
import java.util.Iterator;

/**
 * A set with as few as possible memory usage. Preferred usage: If you need many
 * sets which are close to empty or are read (iterated) way more than written
 * (add/remove). Benefits:
 * <ul>
 * <li>iterator is 4-5 times faster than hash set iterator</li>
 * <li>contains with 2 elements 50% faster, 7-8 elements the speed is similar</li>
 * </ul>
 * Drawbacks:
 * <ul>
 * <li>add/remove is slower, 5-25% (total size 2-10)</li>
 * </ul>
 * So use it only if it fits.
 * 
 * @author stefan franke
 * 
 * @param <T>
 */
public class TinyIdentitySet<T> extends AbstractSet<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Object NOTHING[] = {};
    
    private Object data[] = NOTHING;

    /**
     * @see java.util.Set#size()
     */
    public int size() {
        return data.length;
    }

    /**
     * @see java.util.Set#isEmpty()
     */
    public boolean isEmpty() {
        return data.length == 0;
    }

    /**
     * @see java.util.Set#contains(Object)
     */
    public boolean contains(Object o) {
        for (Object d : data) {
            if (d == o)
                return true;
        }
        return false;
    }

    /**
     * @see java.util.Set#toArray()
     */
    public Object[] toArray() {
        return data.clone();
    }

    /**
     * @see java.util.Set#toArray(Object[])
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] array) {
        if (array.length < data.length) {
            GenericArrayType at = (GenericArrayType) (Type) array.getClass();
            array = (T[]) Array.newInstance((Class<?>) at.getGenericComponentType(), size());
        }
        System.arraycopy(data, 0, array, 0, array.length);
        return array;
    }

    /**
     * @see java.util.Set#add(Object)
     */
    public boolean add(T added) {
        if (contains(added))
            return false;
        Object d2[] = new Object[data.length + 1];
        System.arraycopy(data, 0, d2, 0, data.length);
        d2[data.length] = added;
        data = d2;
        return true;
    }

    /**
     * @see java.util.Set#remove(Object)
     */
    public boolean remove(Object o) {
        for (int i = 0; i < data.length; ++i) {
            Object d = data[i];
            if (d == o) {
                if (data.length == 1) {
                    data = NOTHING;
                } else {
                    Object d2[] = new Object[data.length - 1];
                    System.arraycopy(data, 0, d2, 0, i);
                    System.arraycopy(data, i + 1, d2, i, d2.length - i);
                    data = d2;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * @see java.util.Set#clear()
     */
    public void clear() {
        data = NOTHING;
    }

    /**
     * @see java.util.Set#iterator()
     */
    @Override
    public Iterator<T> iterator() {
        return new AI();
    }

    private class AI implements Iterator<T> {
        private int index = 0;

        /**
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return index < data.length;
        }

        /**
         * @see java.util.Iterator#next()
         */
        @SuppressWarnings("unchecked")
        public T next() {
            return (T) data[index++];
        }

        /**
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            TinyIdentitySet.this.remove(data[--index]);
        }
    }
}
