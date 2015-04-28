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
package org.apache.sis.internal.system;

import java.lang.ref.ReferenceQueue;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link ReferenceQueueConsumer}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class ReferenceQueueConsumerTest extends TestCase {
    /**
     * Verifies that invoking {@link Thread#interrupt()} will cause {@link InterruptedException}
     * to be thrown even if invoked <em>before</em> {@link ReferenceQueue#remove()} put the
     * thread in a waiting state. This behavior is documented in {@link Object#wait()}, but
     * the reference queue javadoc is silent on this topic.
     *
     * <p>This method is not a test of the SIS library, but rather a verification of our JDK
     * library interpretation.</p>
     *
     * @throws InterruptedException This is the excepted exception.
     */
    @Test(expected=InterruptedException.class)
    public void verifyInterruptAssumption() throws InterruptedException {
        final ReferenceQueue<Object> queue = new ReferenceQueue<Object>();
        Thread.currentThread().interrupt();
        assertNull(queue.remove(1000));
    }
}
