/******************************************************************************
 * A pool implementation to manage a distinct count of resources. 
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2015.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *****************************************************************************/

package de.bb.util;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Used to manage objects. The managed objects
 * <ul>
 * <li>must be obtained by key,</li>
 * <li>must be released by key,</li>
 * <li>can be locked for exclusive use,</li>
 * <li>can be validated direct,</li>
 * <li>can be validated indirect, by validating the key</li>
 * </ul>
 * .
 */
public class Pool {
    private long keepAlive;
    private final static boolean DEBUG = false;

    /**
     * Defines the requirements for an object that creates, destroys and validates objects, which are maintained by a
     * Pool.
     */
    public interface Factory {
        /**
         * Create a new object for the Pool.
         * 
         * @throws Exception
         *             an Exception if no object was creatable.
         * @return a new object to be maintained by the Pool.
         */
        public Object create() throws Exception;

        /**
         * Destroy the specified object. After this call, the object is no longer user by the Pool.
         * 
         * @param o
         *            the object to destroy.
         */
        public void destroy(Object o);

        /**
         * Validates the specified object. Used from Pool.release() and Pool.obtain() to ensure that the object is still
         * usable.
         * 
         * @param object
         *            the object to validate.
         * @return true - if object is valid/usable, false either.
         */
        public boolean validate(Object object);

        /* *
         * States hat the specified object is busy, which means it is kept regardless of threshold/max settings.
         * Used from Pool.release() to ensure that busy objects are NOT destroyed.
         * @param object the object to check for idleness.
         * @return true - if object is busy, false either.
         * /
        public boolean isBusy(Object object);
        /**/
        /**
         * Validates the specified key. An invalid key will result into release of its associated object. Used from
         * Pool.validate() which destroys objects that are no longer in use, but somehow no release method was called.
         * 
         * @param key
         *            the key to validate.
         * @return true - if key is valid/usable, false either.
         */
        public boolean validateKey(Object key);
    }

    /** count of unused objects */
    private int threshold = 2;
    /** maximal count of maintained obects */
    private int maxCount = 0x7fffffff;
    /** a stack to maintain all unused objects */
    private HashMap unused = new HashMap();
    /** a hashtable to maintain all used objects */
    private HashMap used = new HashMap();
    /** a hashtabke to maintain the lock counter */
    private HashMap locks = new HashMap();
    /** all waiting threads */
    private Object waiting = new Object();
    /** the factory for the objects. */
    private Pool.Factory factory;

    /**
     * Create a Pool object, with a default timeout of 30 mins.
     * 
     * @param f
     *            a Factory to create, validate and destroy the maintained objects.
     */
    public Pool(Pool.Factory f) {
        this(f, 1000L * 60 * 30);
    }

    /**
     * Create a Pool object.
     * 
     * @param f
     *            a Factory to create, validate and destroy the maintained objects.
     * @param keepAlive
     *            a timeout after unused object are removed and destroyed.
     */
    public Pool(Pool.Factory f, long keepAlive) {
        factory = f;
        this.keepAlive = keepAlive;
    }

    /**
     * Defines how many unused objects are not destroyed. Reducing this value does not immediately cause a reduction of
     * the unused objects!
     * 
     * @param th
     *            set the count of unused objects that are not destroyed
     */
    public void setThreshold(int th) {
        if (th < 0) {
            threshold = 0;
        } else {
            threshold = th;
        }
    }

    /**
     * Set the timeout for unused objects. Setting this value does not immediately affect the timeouts of already unused
     * objects!
     * 
     * @param keepAlive
     *            set the count of unused objects that are not destroyed
     */
    public void setKeepAlive(long keepAlive) {
        this.keepAlive = keepAlive;
    }

    /**
     * Query the current threshold value.
     * 
     * @return the current threshold value.
     */
    public int getthreshold() {
        return threshold;
    }

    /**
     * Defines how many objects are maximal maintained.
     * 
     * @param max
     *            set the max count of objects
     * @see #getMaxCount
     */
    public void setMaxCount(int max) {
        if (max < 0) {
            maxCount = 0;
        } else {
            maxCount = max;
        }
    }

    /**
     * Query the max count of objects in this Pool object.
     * 
     * @return the max count of objects in this Pool object.
     * @see #setMaxCount
     */
    public int getMaxCount() {
        return maxCount;
    }

    /**
     * Get total count of objects in this pool.
     * 
     * @return get the total count of objects in this pool.
     */
    public int size() {
        synchronized (locks) {
            if (DEBUG) {
                System.out.println("count of objects: " + unused.size() + ":" + used.size());
            }
            return unused.size() + used.size();
        }
    }

    /**
     * Obtain an object from the pool. This function waits infinite! If there is an object associated to the key, that
     * same object is returned.
     * 
     * @param key
     *            a key, must not be null.
     * @return an object from the pool.
     * @throws Exception
     *             an exception, if an error occurs.
     */
    public Object obtain(Object key) throws Exception {
        return obtain(key, Integer.MAX_VALUE);
    }

    /**
     * Obtain an object from the pool with a specified max wait time. If there is an object associated to the key, that
     * same object is returned.
     * 
     * @param key
     *            a key, must not be null.
     * @param timeout
     *            a timeout for max wait
     * @return an object from the pool.
     * @throws Exception
     *             an exception, if an error occurs.
     */
    public Object obtain(Object key, long timeout) throws Exception {
        if (DEBUG) {
            System.out.println("obtain-enter unused:used " + unused.size() + ":" + used.size() + " " + key);
        }
        Object o = $obtain(key, timeout);
        this.lock(key);
        if (DEBUG) {
            System.out.println("obtain-leave unused:used " + unused.size() + ":" + used.size() + " " + key);
        }
        return o;
    }

    private Object $obtain(Object key, long timeout) throws Exception {
        long end = timeout + System.currentTimeMillis();
        if (end < timeout)
            end = timeout;
        for (;;) {
            synchronized (locks) {
                // already one for the key?
                Object r = used.get(key);
                if (r != null) {
                    if (factory.validate(r)) {
                        return r;
                    }
                    // kill that object and try again  
                    used.remove(key);
                    factory.destroy(r);
                    continue;
                }

                // one from the stack?
                while (unused.size() > 0) {
                    r = unused.keySet().iterator().next();
                    Long l = (Long) unused.remove(r);
                    if (l.longValue() + keepAlive > System.currentTimeMillis())
                        break;
                    factory.destroy(r); // destroy outdated object 
                    r = null;
                }

                // create a new one?
                if (r == null && used.size() < maxCount) {
                    r = factory.create();
                }

                if (r != null) {
                    used.put(key, r);
                    return r;
                }
            }

            if (DEBUG) {
                System.out.println("obtain-wait unused:used " + unused.size() + ":" + used.size() + " " + key);
            }
            synchronized (waiting) {
                try {
                    if (unused.size() > 0)
                        continue;
                    waiting.wait(timeout);
                } catch (InterruptedException ie) {
                }
                if (System.currentTimeMillis() < end)
                    continue;
            }
            throw new Exception("no object available within specified timeout");
        }
    }

    /**
     * release an object to the pool.
     * 
     * @param key
     *            a key, must not be null.
     * @param o
     *            the releases object.
     * @throws IllegalArgumentException
     *             when the key does not own an object.
     */
    public void release(Object key, Object o) throws IllegalArgumentException {
        if (DEBUG) {
            System.out.println("release-enter unused:used " + unused.size() + ":" + used.size() + " " + key);
        }
        unlock(key);
        $release(key, o);
        if (DEBUG) {
            System.out.println("release-leave unused:used " + unused.size() + ":" + used.size() + " " + key);
        }
    }

    private void $release(Object key, Object o) throws IllegalArgumentException {

        synchronized (locks) {
            // exit if object is locked.
            if (locks.get(key) != null) {
                return;
            }

            // remove it from the used table
            Object r = used.remove(key);
            if (o == null)
                o = r;
            if (r == null || r != o) {
                throw new IllegalArgumentException("key:" + key + ", o=o:" + o + "=" + r);
            }

            // may it be kept unused?
            int uc = unused.size();
            if (factory.validate(o) &&
            //          (!factory.isBusy(o) || 
                    (uc < threshold && uc + used.size() < maxCount)
            //          )
            ) {
                // signal to one to resume
                synchronized (waiting) {
                    unused.put(r, new Long(System.currentTimeMillis()));
                    waiting.notify();
                }
                return;
            }
        }

        // destroy the object
        factory.destroy(o);
    }

    /**
     * Retrieve the object for a given key.
     * 
     * @param key
     *            a key Object - not null
     * @return the object or null
     */
    public Object retrieve(Object key) {
        return used.get(key);
    }

    /**
     * Retrieve the lockCount for a given key.
     * 
     * @param key
     *            a key Object - not null
     * @return the lockCount for a given key
     */
    public int lockCount(Object key) {
        Integer count = (Integer) locks.get(key);
        return count == null ? 0 : count.intValue();
    }

    /**
     * Increase the lock count for an object to grant exclusive usage. As long the lock counter is greater than zero,
     * all obtains with that id will return the same object. Also no other caller may obtain that object.
     * 
     * @param key
     *            the object attached to this key to lock.
     * @return returns current lock count.
     * @throws IllegalArgumentException
     *             when the key does not own an object.
     */
    public int lock(Object key) throws IllegalArgumentException {
        if (used.get(key) == null) {
            throw new IllegalArgumentException("not a valid key: no attached object found");
        }
        int c = 1;
        synchronized (locks) {
            Integer count = (Integer) locks.get(key);
            if (count != null) {
                c = count.intValue() + 1;
            }
            count = new Integer(c);
            locks.put(key, count);
        }
        return c;
    }

    /**
     * decrease the lock count for an object.
     * 
     * @param key
     *            the object attached to this key to lock.
     * @return returns current lock count.
     * @throws IllegalArgumentException
     *             when the key does not own an object.
     */
    public int unlock(Object key) throws IllegalArgumentException {
        if (used.get(key) == null) {
            throw new IllegalArgumentException("not a valid key: no attached object found");
        }
        int c = 0;
        synchronized (locks) {
            Integer count = (Integer) locks.get(key);
            if (count == null) {
                return 0;
            }

            c = count.intValue() - 1;
            count = new Integer(c);
            if (c == 0) {
                locks.remove(key);
            } else {
                locks.put(key, count);
            }
        }
        return c;
    }

    /**
     * Replace the current object by a new object, without changing lock counts. The old object gets destroyed, so it
     * MUST be really unusable.
     * 
     * @param key
     *            the key where the old object is bound.
     * @return the renewed object
     * @throws IllegalArgumentException
     *             when the key does not own an object.
     * @throws Exception
     *             other Exceptions on creation error.
     */
    public Object renew(Object key) throws Exception {
        synchronized (locks) {
            Object o = used.get(key);
            if (o == null) {
                throw new IllegalArgumentException("not a valid key: no attached object found");
            }
            factory.destroy(o);
            o = factory.create();
            used.put(key, o);
            return o;
        }
    }

    /**
     * Validate the current pool. All keys are validated. Object attached to invalid keys are destroyed.
     */
    public void validate() {
        synchronized (locks) {
            for (Iterator i = used.keySet().iterator(); i.hasNext();) {
                Object key = i.next();
                if (!factory.validateKey(key)) {
                    locks.remove(key);
                    Object o = used.remove(key);
                    factory.destroy(o);
                    synchronized (waiting) {
                        waiting.notify();
                    }
                }
            }
        }
    }
}
