/******************************************************************************
 * Manage (start/stop) threads for a distinct implementation.  
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

/**
 * Used to manage threads for the same task, when the count of needed tasks changes with the time. A good example is a
 * HTTP server where each request gets an own task to process the request and computes the output. It's easy
 * understandable that the count of needed threads depends on the incoming requests, the duration of the processing and
 * so on. <br>
 * The complete management is done by this class, only the count of waiting threads, ready to catch the next incoming
 * request, and a maximum count of active threads, to limit and protect from overload must be configured. <br>
 * An important hint is: Don?t use more waiting threads for one task than available CPU.
 * <hr>
 * An example:
 * 
 * <pre>
 * import de.bb.util.*;
 * 
 * public class TmTest {
 * 
 *     static class F implements ThreadManager.Factory {
 *         public void create(ThreadManager tm) {
 *             new T(tm).start(); // create the thread and start the thread
 *         }
 *     }
 * 
 *     static class T extends ThreadManager.Thread {
 *         T(ThreadManager tm) {
 *             super(tm);
 *         }
 * 
 *         public void run() {
 *             // this endless loop is a MUST
 *             while (!mustDie()) {
 *                 try {
 *                     // wait for trigger
 *                     int t = 1000 + (int) (Math.random() * 2000);
 *                     System.out.println(&quot;sleeping : &quot; + t);
 *                     sleep(t);
 * 
 *                     // now start to do something
 *                     setBusy(); // checks also whether threads must be added
 * 
 *                     // simulate work
 *                     t = 1000 + (int) (Math.random() * 2000);
 *                     System.out.println(&quot;working : &quot; + t);
 *                     sleep(t);
 * 
 *                 } catch (Throwable t) // you also MUST catch EVERYTHING!
 *                 {
 *                 }
 *             }
 *             System.out.println(&quot;dead ++++++++++++&quot;);
 *         }
 * 
 *     }
 * 
 *     // example main
 *     public static void main(String args[]) {
 *         ThreadManager tm = new ThreadManager(new F());
 *         tm.setMaxCount(20);
 * 
 *         try {
 * 
 *             tm.setWaitCount(3);
 *             System.out.println(&quot;3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 &quot;);
 *             Thread.sleep(10 * 1000);
 * 
 *             tm.setWaitCount(10);
 *             System.out.println(&quot;10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 &quot;);
 *             Thread.sleep(10 * 1000);
 * 
 *             tm.setWaitCount(5);
 *             System.out.println(&quot;5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5&quot;);
 *             Thread.sleep(10 * 1000);
 * 
 *             tm.setMaxCount(10);
 *             System.out.println(&quot;m10 m10 m10 m10 m10 m10 m10 m10 m10 m10 m10 m10 m10 m10 m10 m10 m10 m10&quot;);
 *             Thread.sleep(10 * 1000);
 * 
 *             tm.setMaxCount(0);
 *             System.out.println(&quot;0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 &quot;);
 *             while (tm.size() &gt; 0)
 *                 Thread.sleep(1000);
 *         } catch (Exception e) {
 *         }
 *     }
 * }
 * </pre>
 */

// / the thread managing class
public class ThreadManager {
    private final static boolean DEBUG = false;

    /**
     * Interface which contains the thread creating function. The implementation has to create a thread and to start it.
     */
    public static interface Factory {
        /**
         * The implementation either creates a new thread and starts it.
         * 
         * @param tm
         *            the thread manager where each thread reports its state.
         */
        public void create(ThreadManager tm);
    }

    /**
     * Abstract class for all threads used with the ThreadManager.
     */
    public static class Thread extends java.lang.Thread {
        // / the used thread manager
        private ThreadManager tm = null;

        // / request the termination of the thread
        private boolean terminate = false;
        // / are we busy?
        private boolean busy = false;

        /**
         * Ct to create a manageable thread.
         * 
         * @param tm
         *            the ThreadManager object
         */
        public Thread(ThreadManager tm) {
            super(tm.threadGroup, tm.mid + "-thread-" + (++tm.tid));
            this.tm = tm;
            busy = true;
        }

        /**
         * This function MUST be called from the threads main loop BEFORE the real work starts.
         */
        protected void setBusy() {
            if (!busy) {
                busy = true;
                tm.setBusy();
            }
        }

        /**
         * Force termination of the current thread. Next call of mustDie() will return true;
         */
        protected void requestDie() {
            terminate = true;
        }

        /**
         * This function MUST be polled during the threads run loop.
         * 
         * @return true if the thread must terminate its work.
         */
        protected boolean mustDie() {
            if (!busy)
                tm.setBusy();
            busy = false;
            return tm.mustDie(terminate);
        }

        /**
         * Is the current thread busy? This functions gives only then correct information, if the functions setBusy()
         * and mustDie() are used correctly.
         * 
         * @return true if the current thread does some real work.
         */
        public boolean isBusy() {
            return busy;
        }
        
        public void idle() {
            if (busy)
                return;
            
            synchronized (tm.idle) {
                try {
                    ++ tm.idling;
                    tm.idle.wait();
                } catch (InterruptedException e) {
                } finally {
                    -- tm.idling;
                }
            }
            setBusy();
        }
    }

    private int maxWaitCount = 1;
    private int maxCount = 0;
    private int waiting = 0;
    private int running = 0;

    // used to create threads
    private Factory factory;

    private static int globalMid = 0;
    String mid;
    int tid = 0;
    // a thread group for all threads - used to detrmine the real count of threads!
    ThreadGroup threadGroup = new ThreadGroup("ThreadManager #" + mid);

    private Object lock = new Object();

    private long nextReduceTime;
    
    Object idle = new Object();
    int idling;

    /**
     * Create a ThreadManager object.
     * 
     * @param f
     *            a factory to create the managed threads.
     */
    public ThreadManager(Factory f) {
        this("bb_threadmanager-" + (++globalMid), f);
    }

    /**
     * Create a ThreadManager object.
     * 
     * @param name
     *            a name shown in debug thread view.
     * @param f
     *            a factory to create the managed threads.
     */
    public ThreadManager(String name, Factory f) {
        mid = name;
        factory = f;
    }

    /**
     * Defines how many threads are waiting.
     * 
     * @param wc
     *            set the count of waiting threads
     */
    public void setWaitCount(int wc) {
        if (wc < 1)
            wc = 1;
        synchronized (lock) {
            if (wc > maxCount)
                wc = maxCount;
            maxWaitCount = wc;
        }
        check();
    }

    public int getMaxWaitCount() {
        return maxWaitCount;
    }

    public int getWaitCount() {
        return waiting;
    }

    /**
     * Defines how many threads are maximal available.
     * 
     * @param max
     *            set the max count of threads
     * @see #getMaxCount
     */
    public void setMaxCount(int max) {
        if (max < 0)
            max = 0;
        synchronized (lock) {
            maxCount = max;
            if (maxWaitCount > max)
                maxWaitCount = max;
        }
        check();
    }

    /**
     * Query many threads are maximal available.
     * 
     * @return the max count of threads
     * @see #setMaxCount
     */
    public int getMaxCount() {
        return maxCount;
    }

    /**
     * Switch a waiting thread to running mode.
     */
    void setBusy() {
        synchronized (lock) {
            --waiting;
            ++running;
            if (DEBUG)
                System.out.println("wait -> running - now: " + running + ":" + waiting);
            check();
        }
    }

    /**
     * Detect whether a thread must terminate.
     * 
     * @param die
     *            true when the thread requests termination
     * @return true if the thread MUST terminate
     */
    boolean mustDie(boolean die) {
        synchronized (lock) {

            // do not exceed the maxCount
            die |= waiting + running > maxCount;

            // time has passed - reduce if waiting > maxWaitCount 
            if (!die && System.currentTimeMillis() > nextReduceTime) {
                die = waiting > maxWaitCount;
            }

            --running;
            if (die) {
                wakeIdle();
            } else {
                ++waiting;
            }

            // if (force) // check whether a new thread must be launched
            check();

            if (DEBUG)
                System.out
                        .println("mustDie : " + die + " " + running + "+" + waiting + "=" + threadGroup.activeCount());
            return die;
        }
    }

    /**
     * Get total count of threads.
     * 
     * @return get the total count of threads
     */
    public int size() {
        synchronized (lock) {
            if (DEBUG)
                System.out.println("total         : " + running + ":" + waiting + "=" + threadGroup.activeCount());

            if (threadGroup.activeCount() < running + waiting) {
                System.out.println("fixing total: " + running + ":" + waiting + "=" + threadGroup.activeCount());
                waiting = running - threadGroup.activeCount();
                if (waiting < 0)
                    waiting = 0;
            }
            
            return running + waiting;
        }
    }

    /**
     * Replace each running thread with a new thread. Which means each running thread is terminated, and a new thread is
     * started.
     */
    public void renew() {
        synchronized (lock) {
            Thread[] threads = new Thread[threadGroup.activeCount()];
            threadGroup.enumerate(threads);
            for (int i = 0; i < threads.length; ++i) {
                threads[i].requestDie();
            }
        }
    }

    /**
     * Checks whether threads must be created/destroyed/launched.
     */
    private void check() {
        synchronized (lock) {
            // create needed threads
            if (waiting >= maxWaitCount)
                return;

            // check whether threads are lost:
            if (running + waiting > threadGroup.activeCount()) {
                // System.out.println("fixup running thread count by " + (threadGroup.activeCount() - waiting));
                running = threadGroup.activeCount() - waiting;
            }

            nextReduceTime = System.currentTimeMillis() + 60 * 1000L;
            
            // create threads
            int max = maxWaitCount - waiting - idling;
            if (max < 0)
                max = 0;
            if (running + waiting + max > maxCount)
                max = maxCount - running - waiting;
            try {
                while (max-- > 0) {
                    ++running;
                    factory.create(this);
                    if (DEBUG)
                        System.out.println("creating thread - now: " + running + ":" + waiting);
                    // Thread.yield();
                }
            } catch (Throwable t) {
                --running;
                t.printStackTrace();
            }
        }

    }

    /**
     * Return the count of running threads.
     * 
     * @return running count.
     */
    public int getRunning() {
        return running;
    }
    
    public void wakeIdle() {
        synchronized (idle) {
            idle.notifyAll();
        }
    }
}
