/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.util.collection;

import java.util.Map;
import java.util.HashMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.sis.math.Statistics;
import org.apache.sis.math.StatisticsFormat;
import org.apache.sis.util.CharSequences;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.Performance;
import org.junit.Test;

import static java.lang.StrictMath.*;
import static java.util.Collections.singleton;
import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link Cache} with simple tests and a {@linkplain #stress() stress} test.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(WeakValueHashMapTest.class)
public final strictfp class CacheTest extends TestCase {
    /**
     * Tests {@link Cache} as a {@link java.util.Map} using strong references.
     * The tested {@code Cache} shall behave like a standard {@link HashMap},
     * except for element order.
     *
     * @see WeakValueHashMapTest#testStrongReferences()
     */
    @Test
    public void testStrongReferences() {
        WeakValueHashMapTest.testStrongReferences(
                new Cache<Integer,Integer>(WeakValueHashMapTest.SAMPLE_SIZE, 0, false));
    }

    /**
     * Tests {@link Cache} as a {@link java.util.Map} using weak references. In this test, we
     * have to keep in mind than some elements in {@code weakMap} may disappear at any time.
     *
     * @throws InterruptedException If the test has been interrupted.
     *
     * @see WeakValueHashMapTest#testWeakReferences()
     */
    @Test
    @DependsOnMethod("testStrongReferences")
    public void testWeakReferences() throws InterruptedException {
        WeakValueHashMapTest.testWeakReferences(
                new Cache<Integer,Integer>(WeakValueHashMapTest.SAMPLE_SIZE, 0, false));
    }

    /**
     * Tests adding a single value using the {@link Cache.Handler#putAndUnlock(Object)} method.
     * This method does all the operations in a single thread.
     */
    @Test
    public void testPutAndUnlock() {
        final String key   = "The key";
        final String value = "The value";
        final Cache<String,String> cache = new Cache<String,String>();
        assertTrue("No initial value expected.", cache.isEmpty());
        assertNull("No initial value expected.", cache.peek(key));

        final Cache.Handler<String> handler = cache.lock(key);
        assertNull("No initial value expected.", handler.peek());
        handler.putAndUnlock(value);

        assertEquals(1,              cache.size());
        assertEquals(value,          cache.peek(key));
        assertEquals(singleton(key), cache.keySet());
        assertEquals(singleton(new SimpleEntry<String,String>(key, value)), cache.entrySet());
    }

    /**
     * Tests the cache when a thread is blocking a second one.
     * The second thread tries to write a value while the first thread holds the lock.
     *
     * @throws InterruptedException If the test has been interrupted.
     */
    @Test
    @DependsOnMethod("testPutAndUnlock")
    public void testThreadBlocking() throws InterruptedException {
        final String    keyByMainThread =    "keyByMainThread";
        final String  valueByMainThread =  "valueByMainThread";
        final String   keyByOtherThread =   "keyByOtherThread";
        final String valueByOtherThread = "valueByOtherThread";
        final Cache<String,String> cache = new Cache<String,String>();
        final class OtherThread extends Thread {
            /**
             * If an error occurred, the cause. It may be an {@link AssertionError}.
             */
            Throwable failure;

            /**
             * Creates a new thread.
             */
            OtherThread() {
                super(TestUtilities.THREADS, "CacheTest.testThreadBlocking()");
            }

            /**
             * Reads the value added by the main thread, then adds an other value.
             * The first operation shall block while the main thread holds the lock.
             */
            @Override public void run() {
                try {
                    final Cache.Handler<String> handler = cache.lock(keyByMainThread);
                    assertTrue(handler instanceof Cache<?,?>.Work.Wait);
                    assertSame(valueByMainThread, handler.peek());
                    handler.putAndUnlock(valueByMainThread);
                    assertSame(valueByMainThread, cache.peek(keyByMainThread));
                } catch (Throwable e) {
                    failure = e;
                }
                try {
                    final Cache.Handler<String> handler = cache.lock(keyByOtherThread);
                    assertTrue(handler instanceof Cache<?,?>.Work);
                    assertNull(handler.peek());
                    handler.putAndUnlock(valueByOtherThread);
                    assertSame(valueByOtherThread, cache.peek(keyByOtherThread));
                } catch (Throwable e) {
                    if (failure == null) {
                        failure = e;
                    } else {
                        // The JDK7 branch invokes Throwable.addSuppressed(…) here.
                    }
                }
            }
        }
        /*
         * Gets the lock, then start the second thread which will try to write
         * a value for the same key. The second thread shall block.
         */
        final Cache.Handler<String> handler = cache.lock(keyByMainThread);
        assertTrue(handler instanceof Cache<?,?>.Work);
        final OtherThread thread = new OtherThread();
        thread.start();
        TestUtilities.waitForBlockedState(thread);
        assertNull("The blocked thread shall not have added a value.", cache.peek(keyByOtherThread));
        /*
         * Write. This will release the lock and let the other thread continue its job.
         */
        handler.putAndUnlock(valueByMainThread);
        thread.join();
        TestUtilities.rethrownIfNotNull(thread.failure);
        /*
         * Checks the map content.
         */
        final Map<String,String> expected = new HashMap<String,String>(4);
        assertNull(expected.put( keyByMainThread,  valueByMainThread));
        assertNull(expected.put(keyByOtherThread, valueByOtherThread));
        assertMapEquals(expected, cache);
    }

    /**
     * Validates the entries created by the {@link #stress()} test. The check performed in
     * this method shall obviously be consistent with the values created by {@code stress()}.
     *
     * @param  name  The name of the value being measured.
     * @param  cache The cache to validate.
     * @return Statistics on the key values of the given map.
     */
    private static Statistics validateStressEntries(final String name, final Map<Integer,Integer> cache) {
        final Statistics statistics = new Statistics(name);
        for (final Map.Entry<Integer,Integer> entry : cache.entrySet()) {
            final int key = entry.getKey();
            final int value = entry.getValue();
            assertEquals(key*key, value);
            statistics.accept(key);
        }
        return statistics;
    }

    /**
     * Starts many threads writing in the same cache, with a high probability that two threads
     * ask for the same key in some occasions.
     *
     * @throws InterruptedException If the test has been interrupted.
     */
    @Test
    @Performance
    @DependsOnMethod("testThreadBlocking")
    public void stress() throws InterruptedException {
        final int count = 5000;
        final Cache<Integer,Integer> cache = new Cache<Integer,Integer>();
        final AtomicReference<Throwable> failures = new AtomicReference<Throwable>();
        final class WriterThread extends Thread {
            /**
             * Incremented every time a value has been added. This is not the number of time the
             * loop has been executed, since this variable is not incremented when a value already
             * exists for a key.
             */
            int addCount;

            /**
             * Creates a new thread.
             */
            WriterThread(final int i) {
                super(TestUtilities.THREADS, "CacheTest.stress() #" + i);
            }

            /**
             * Put random values in the map.
             */
            @Override public void run() {
                for (int i=0; i<count; i++) {
                    final Integer key = i;
                    final Integer expected = new Integer(i * i); // We really want new instance.
                    final Integer value;
                    try {
                        value = cache.getOrCreate(key, new Callable<Integer>() {
                            @Override public Integer call() {
                                return expected;
                            }
                        });
                        assertEquals(expected, value);
                    } catch (Throwable e) {
                        if (!failures.compareAndSet(null, e)) {
                            // The JDK7 branch invokes Throwable.addSuppressed(…) here.
                        }
                        continue;
                    }
                    if (expected == value) { // Identity comparison (not value comparison).
                        addCount++;
                        yield(); // Gives a chance to other threads.
                    }
                }
            }
        }
        final WriterThread[] threads = new WriterThread[50];
        for (int i=0; i<threads.length; i++) threads[i] = new WriterThread(i);
        for (int i=0; i<threads.length; i++) threads[i].start();
        for (int i=0; i<threads.length; i++) threads[i].join();
        TestUtilities.rethrownIfNotNull(failures.get());
        /*
         * Verifies the values.
         */
        final Statistics beforeGC = validateStressEntries("Before GC", cache);
        assertTrue("Should not have more entries than what we put in.", cache.size() <= count);
        assertFalse("Some entries should be retained by strong references.", cache.isEmpty());
        /*
         * If verbose test output is enabled, report the number of cache hits.
         * The numbers below are for tuning the test only. The output is somewhat
         * random so we can not check it in a test suite.  However if the test is
         * properly tuned, most values should be non-zero.
         */
        final PrintWriter out = CacheTest.out;
        TestUtilities.printSeparator("CacheTest.stress() - testing concurrent accesses");
        out.print("There is "); out.print(threads.length); out.print(" threads, each of them"
                + " fetching or creating "); out.print(count); out.println(" values.");
        out.println("Number of times a new value has been created, for each thread:");
        for (int i=0; i<threads.length;) {
            final String n = String.valueOf(threads[i++].addCount);
            out.print(CharSequences.spaces(6 - n.length()));
            out.print(n);
            if ((i % 10) == 0) {
                out.println();
            }
        }
        out.println();
        out.println("Now observe how the background thread cleans the cache.");
        long time = System.nanoTime();
        for (int i=0; i<10; i++) {
            final long t = System.nanoTime();
            out.printf("Cache size: %4d (after %3d ms)%n", cache.size(), round((t - time) / 1E+6));
            time = t;
            Thread.sleep(250);
            if (i >= 2) {
                System.gc();
            }
        }
        out.println();
        /*
         * Gets the statistics of key values after garbage collection. Most values should
         * be higher, because oldest values (which should have been garbage collected first)
         * have lower values. If verbose output is enabled, then we will print the statistics
         * before to perform the actual check in order to allow the developer to have more
         * information in case of failure.
         *
         * The mean value is often greater, but not always. Since we have fewer remaining values
         * (100 instead of 10000), the remaining low values will have a much greater impact on
         * the mean. Only the check on the minimal value is fully reliable.
         */
        final Statistics afterGC = validateStressEntries("After GC", cache);
        out.println("Statistics on the keys before and after garbage collection.");
        out.println("The minimum value shall always be equals or greater after GC.");
        out.println("The mean value is usually greater too, except by coincidence.");
        final StatisticsFormat format = StatisticsFormat.getInstance();
        format.setBorderWidth(1);
        try {
            format.format(new Statistics[] {beforeGC, afterGC}, out);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        assertTrue("Minimum key value should be greater after garbage collection.",
                afterGC.minimum() >= beforeGC.minimum());
    }
}
