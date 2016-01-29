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
import org.opengis.util.FactoryException;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;


/**
 * Tests {@link ConcurrentAuthorityFactory}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn(AuthorityFactoryProxyTest.class)
public final strictfp class ConcurrentAuthorityFactoryTest extends TestCase {
    /**
     * The timeout used for this test.
     */
    private static final long TIMEOUT = ConcurrentAuthorityFactory.TIMEOUT_RESOLUTION * 4;

    /**
     * A concurrent factory which creates new instances of {@link AuthorityFactoryMock}.
     */
    private static final strictfp class Mock extends ConcurrentAuthorityFactory<AuthorityFactoryMock> {
        /** All factories created by this mock, including any factories having been disposed. */
        private final Queue<AuthorityFactoryMock> allDAOs = new ConcurrentLinkedQueue<AuthorityFactoryMock>();

        /** Creates a new concurrent authority factory. */
        Mock() {
            super(AuthorityFactoryMock.class);
            setTimeout(TIMEOUT, TimeUnit.NANOSECONDS);
        }

        /** Invoked when a new factory needs to be created. */
        @Override protected AuthorityFactoryMock newDataAccess() {
            assertFalse("Should be invoked outside synchronized block.", Thread.holdsLock(this));
            final AuthorityFactoryMock factory = new AuthorityFactoryMock("Mock", null);
            assertTrue(allDAOs.add(factory));
            return factory;
        }

        /** Returns a copy of the factories queue. */
        final synchronized List<AuthorityFactoryMock> createdDAOs() {
            return new ArrayList<AuthorityFactoryMock>(allDAOs);
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
        assertTrue  ("Should have initially no DAO.", factory.createdDAOs().isEmpty());
        assertEquals("Should have initially no DAO.", 0, factory.countAvailableDataAccess());
        assertNotNull(factory.createObject("84"));

        List<AuthorityFactoryMock> createdDAOs = factory.createdDAOs();
        assertEquals("Expected a new DAO.",         1, createdDAOs.size());
        assertEquals("Expected one valid DAO.",     1, factory.countAvailableDataAccess());
        assertFalse ("Should not be disposed yet.", createdDAOs.get(0).isClosed());

        sleepWithoutExceedingTimeout(workTime, 2 * ConcurrentAuthorityFactory.TIMEOUT_RESOLUTION);
        assertEquals("Expected no new DAO.",        createdDAOs, factory.createdDAOs());
        assertEquals("Expected one valid DAO.",     1, factory.countAvailableDataAccess());
        assertFalse ("Should not be disposed yet.", createdDAOs.get(0).isClosed());

        sleepUntilAfterTimeout(3 * ConcurrentAuthorityFactory.TIMEOUT_RESOLUTION, factory);
        assertEquals("Expected no new DAO.",       createdDAOs, factory.createdDAOs());
        assertEquals("Worker should be disposed.", 0, factory.countAvailableDataAccess());
        assertTrue  ("Worker should be disposed.", createdDAOs.get(0).isClosed());
        /*
         * Ask again for the same object and check that no new DAO
         * were created because the value was taken from the cache.
         */
        assertNotNull(factory.createObject("84"));
        assertEquals("Expected no new DAO.",      createdDAOs, factory.createdDAOs());
        assertEquals("Worker should be disposed.", 0, factory.countAvailableDataAccess());
        /*
         * Ask for one element and check that a new DAO is created.
         */
        workTime = System.nanoTime();
        assertNotNull(factory.createObject("4326"));
        createdDAOs = factory.createdDAOs();
        assertEquals("Expected a new DAO.",         2, createdDAOs.size());
        assertEquals("Expected one valid DAO.",     1, factory.countAvailableDataAccess());
        assertFalse ("Should not be disposed yet.", createdDAOs.get(1).isClosed());

        sleepWithoutExceedingTimeout(workTime, 2 * ConcurrentAuthorityFactory.TIMEOUT_RESOLUTION);
        assertEquals("Expected no new DAO.",        createdDAOs, factory.createdDAOs());
        assertEquals("Expected one valid DAO.",     1, factory.countAvailableDataAccess());
        assertFalse ("Should not be disposed yet.", createdDAOs.get(1).isClosed());
        /*
         * Ask again for a new element before the timeout is elapsed and check
         * that the disposal of the Data Access Objects has been reported.
         */
        workTime = System.nanoTime();
        assertNotNull(factory.createObject("4979"));
        sleepWithoutExceedingTimeout(workTime, ConcurrentAuthorityFactory.TIMEOUT_RESOLUTION);
        assertNotNull(factory.createObject("5714"));
        sleepWithoutExceedingTimeout(workTime, 2 * ConcurrentAuthorityFactory.TIMEOUT_RESOLUTION);
        assertEquals("Expected one valid DAO.",     1, factory.countAvailableDataAccess());
        assertFalse ("Should not be disposed yet.", createdDAOs.get(1).isClosed());
        assertEquals("Expected no new DAO.",        createdDAOs, factory.createdDAOs());

        sleepUntilAfterTimeout(3 * ConcurrentAuthorityFactory.TIMEOUT_RESOLUTION, factory);
        assertEquals("Expected no new DAO.",        createdDAOs, factory.createdDAOs());
        assertEquals("Worker should be disposed.",  0, factory.countAvailableDataAccess());
        assertTrue  ("Worker should be disposed.",  createdDAOs.get(1).isClosed());             // TODO: sometime not yet disposed.
        assertTrue  ("Worker should be disposed.",  createdDAOs.get(0).isClosed());
    }

    /**
     * Sleeps and ensures that the sleep time did not exceeded the timeout. The sleep time could be greater if the test
     * machine is under heavy load (for example a Jenkins server), in which case we will cancel the test without failure.
     */
    private static void sleepWithoutExceedingTimeout(final long previousTime, final long waitTime) throws InterruptedException {
        Thread.sleep(TimeUnit.NANOSECONDS.toMillis(waitTime));
        assumeTrue(System.nanoTime() - previousTime < TIMEOUT);
    }

    /**
     * Sleeps a time long enough so that we exceed the timeout time. After this method call, the
     * DAOs should be disposed. However if they are not, then we will wait a little bit more.
     *
     * <p>The workers should be disposed right after the sleep time. However the workers disposal is performed
     * by a shared (SIS-library wide) daemon thread. Because the later is invoked in a background thread,
     * it is subject to the hazard of thread scheduling.</p>
     */
    private static void sleepUntilAfterTimeout(final long waitTime, final ConcurrentAuthorityFactory<?> factory)
            throws InterruptedException
    {
        Thread.sleep(TimeUnit.NANOSECONDS.toMillis(waitTime));
        int n = 3;
        while (factory.isCleanScheduled()) {
            Logging.getLogger("org.geotoolkit.referencing.factory")
                    .warning("Execution of ConcurrentAuthorityFactory.disposeExpired() has been delayed.");
            Thread.sleep(TIMEOUT);
            System.gc();
            if (--n == 0) {
                break;
            }
        }
    }
}
