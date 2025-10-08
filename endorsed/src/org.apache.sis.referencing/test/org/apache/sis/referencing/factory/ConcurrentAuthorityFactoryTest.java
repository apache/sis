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
package org.apache.sis.referencing.factory;

import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.Field;
import org.opengis.util.FactoryException;
import static org.apache.sis.util.internal.shared.Constants.NANOS_PER_MILLISECOND;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link ConcurrentAuthorityFactory}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class ConcurrentAuthorityFactoryTest extends TestCase {
    /**
     * The timeout used for this test, in nanoseconds.
     */
    private static final long TIMEOUT = ConcurrentAuthorityFactory.TIMEOUT_RESOLUTION * 4;

    /**
     * Creates a new test case.
     */
    public ConcurrentAuthorityFactoryTest() {
    }

    /**
     * Verifies the value of {@code ConcurrentAuthorityFactory.Finder.DOMAIN_COUNT}.
     * This method uses reflection because the verified class is private.
     *
     * @throws ReflectiveOperationException if the class name or field name are not as expected.
     */
    @Test
    public void verifyDomainCount() throws ReflectiveOperationException {
        final Class<?> c = Class.forName(ConcurrentAuthorityFactory.class.getName() + "$Finder");
        final Field f = c.getDeclaredField("DOMAIN_COUNT");
        f.setAccessible(true);
        assertEquals(IdentifiedObjectFinder.Domain.values().length, f.getInt(null));
    }

    /**
     * A concurrent factory which creates new instances of {@link AuthorityFactoryMock}.
     */
    private static final class Mock extends ConcurrentAuthorityFactory<AuthorityFactoryMock> {
        /** All factories created by this mock, including any factories having been disposed. */
        private final Queue<AuthorityFactoryMock> allDAOs = new ConcurrentLinkedQueue<>();

        /** Creates a new concurrent authority factory. */
        Mock() {
            super(AuthorityFactoryMock.class);
            setTimeout(TIMEOUT, TimeUnit.NANOSECONDS);
        }

        /** Invoked when a new factory needs to be created. */
        @Override protected AuthorityFactoryMock newDataAccess() {
            assertFalse(Thread.holdsLock(this), "Should be invoked outside synchronized block.");
            final AuthorityFactoryMock factory = new AuthorityFactoryMock("Mock", null);
            assertTrue(allDAOs.add(factory));
            return factory;
        }

        /** Returns a copy of the factories queue. */
        final synchronized List<AuthorityFactoryMock> createdDAOs() {
            return new ArrayList<>(allDAOs);
        }
    }


    /**
     * Tests the disposal of Data Access Objects (DAO) after the timeout.
     *
     * @throws FactoryException should never happen.
     * @throws InterruptedException if the test has been interrupted.
     */
    @Test
    public void testTimeout() throws FactoryException, InterruptedException {
        final Mock factory = new Mock();
        /*
         * Ask for one element, wait for the timeout and check that the DAO is disposed.
         */
        long workTime = System.nanoTime();
        assertTrue  (factory.createdDAOs().isEmpty(),       "Should have initially no DAO.");
        assertEquals(0, factory.countAvailableDataAccess(), "Should have initially no DAO.");
        assertNotNull(factory.createObject("84"));

        List<AuthorityFactoryMock> createdDAOs = factory.createdDAOs();
        assertEquals(1, createdDAOs.size(),                 "Expected a new DAO.");
        assertEquals(1, factory.countAvailableDataAccess(), "Expected one valid DAO.");
        assertFalse (createdDAOs.get(0).isClosed(),         "Should not be disposed yet.");

        sleepWithoutExceedingTimeout(workTime, 2 * ConcurrentAuthorityFactory.TIMEOUT_RESOLUTION);
        assertEquals(createdDAOs, factory.createdDAOs(),    "Expected no new DAO.");
        assertEquals(1, factory.countAvailableDataAccess(), "Expected one valid DAO.");
        assertFalse (createdDAOs.get(0).isClosed(),         "Should not be disposed yet.");

        final boolean r1 = sleepUntilAfterTimeout(3 * ConcurrentAuthorityFactory.TIMEOUT_RESOLUTION, factory);
        if (r1) {
            assertEquals(createdDAOs, factory.createdDAOs(),    "Expected no new DAO.");
            assertEquals(0, factory.countAvailableDataAccess(), "Worker should be disposed.");
            assertTrue  (createdDAOs.get(0).isClosed(),         "Worker should be disposed.");
        }
        /*
         * Ask again for the same object and check that no new DAO
         * were created because the value was taken from the cache.
         */
        assertNotNull(factory.createObject("84"));
        assertEquals(createdDAOs, factory.createdDAOs(),    "Expected no new DAO.");
        assertEquals(0, factory.countAvailableDataAccess(), "Worker should be disposed.");
        /*
         * Ask for one element and check that a new DAO is created.
         */
        workTime = System.nanoTime();
        assertNotNull(factory.createObject("4326"));
        createdDAOs = factory.createdDAOs();
        assertEquals(2, createdDAOs.size(),                 "Expected a new DAO.");
        assertEquals(1, factory.countAvailableDataAccess(), "Expected one valid DAO.");
        assertFalse (createdDAOs.get(1).isClosed(),         "Should not be disposed yet.");

        sleepWithoutExceedingTimeout(workTime, 2 * ConcurrentAuthorityFactory.TIMEOUT_RESOLUTION);
        assertEquals(createdDAOs, factory.createdDAOs(),    "Expected no new DAO.");
        assertEquals(1, factory.countAvailableDataAccess(), "Expected one valid DAO.");
        assertFalse (createdDAOs.get(1).isClosed(),         "Should not be disposed yet.");
        /*
         * Ask again for a new element before the timeout is elapsed and check
         * that the disposal of the Data Access Objects has been reported.
         */
        workTime = System.nanoTime();
        assertNotNull(factory.createObject("4979"));
        sleepWithoutExceedingTimeout(workTime, ConcurrentAuthorityFactory.TIMEOUT_RESOLUTION);
        assertNotNull(factory.createObject("5714"));
        sleepWithoutExceedingTimeout(workTime, 2 * ConcurrentAuthorityFactory.TIMEOUT_RESOLUTION);
        assertEquals(1, factory.countAvailableDataAccess(), "Expected one valid DAO.");
        assertFalse (createdDAOs.get(1).isClosed(),         "Should not be disposed yet.");
        assertEquals(createdDAOs, factory.createdDAOs(),    "Expected no new DAO.");

        final boolean r2 = sleepUntilAfterTimeout(3 * ConcurrentAuthorityFactory.TIMEOUT_RESOLUTION, factory);
        if (r2) {
            assertEquals(createdDAOs, factory.createdDAOs(),    "Expected no new DAO.");
            assertEquals(0, factory.countAvailableDataAccess(), "Worker should be disposed.");
            assertTrue  (createdDAOs.get(1).isClosed(),         "Worker should be disposed.");
            assertTrue  (createdDAOs.get(0).isClosed(),         "Worker should be disposed.");
        }
        // If the garbage collector didn't complete, report as a skipped test instead of a test failure.
        assumeTrue(r1 & r2, "The execution of ConcurrentAuthorityFactory.disposeExpired() could not complete.");
    }

    /**
     * Sleeps and ensures that the sleep time did not exceeded the timeout. The sleep time could be greater if the test
     * machine is under heavy load (for example a Jenkins server), in which case we will cancel the test without failure.
     * All times are in nanoseconds.
     */
    private static void sleepWithoutExceedingTimeout(final long previousTime, final long waitTime) throws InterruptedException {
        Thread.sleep(TimeUnit.NANOSECONDS.toMillis(waitTime));
        assumeTrue(System.nanoTime() - previousTime < TIMEOUT, "Test skipped because it took longer than expected.");
    }

    /**
     * Sleeps a time long enough so that we exceed the timeout time. After this method call,
     * the DAOs should be disposed. However if they are not, then we will wait a little bit more.
     *
     * <p>The workers should be disposed right after the sleep time. However, the workers disposal is performed
     * by a shared (SIS-library wide) daemon thread. Because the latter is invoked in a background thread,
     * it is subject to the hazard of thread scheduling.</p>
     *
     * @param  waitTime  the time to wait, in nanoseconds.
     * @return {@code true} if all pending factories have been disposed.
     */
    @SuppressWarnings("SleepWhileInLoop")
    private static boolean sleepUntilAfterTimeout(final long waitTime, final ConcurrentAuthorityFactory<?> factory)
            throws InterruptedException
    {
        Thread.sleep(TimeUnit.NANOSECONDS.toMillis(waitTime));
        int n = 3;
        while (factory.isCleanScheduled()) {
            Thread.sleep(TIMEOUT / NANOS_PER_MILLISECOND);
            System.gc();
            if (--n == 0) {
                return false;
            }
        }
        return true;
    }
}
