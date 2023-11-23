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
package org.apache.sis.io.stream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Random;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;

// Test dependencies
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;


/**
 * Tests {@link UpdatableWrite}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class UpdatableWriteTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public UpdatableWriteTest() {
    }

    /**
     * Tests {@link UpdatableWrite#of(ChannelDataOutput, short)}.
     *
     * @throws IOException should never happen since we are writing in memory.
     */
    @Test
    public void testWithShorts() throws IOException {
        randomTests(Short.BYTES, (random) -> (short) random.nextInt(), ByteBuffer::putShort, (output, value) -> {
            try {
                return UpdatableWrite.of(output, value);
            } catch (IOException e) {
                throw new AssertionError(e);        // Should never happen in this test.
            }
        });
    }

    /**
     * Tests {@link UpdatableWrite#of(ChannelDataOutput, int)}.
     *
     * @throws IOException should never happen since we are writing in memory.
     */
    @Test
    public void testWithInts() throws IOException {
        randomTests(Integer.BYTES, Random::nextInt, ByteBuffer::putInt, (output, value) -> {
            try {
                return UpdatableWrite.of(output, value);
            } catch (IOException e) {
                throw new AssertionError(e);        // Should never happen in this test.
            }
        });
    }

    /**
     * Tests {@link UpdatableWrite#of(ChannelDataOutput, long)}.
     *
     * @throws IOException should never happen since we are writing in memory.
     */
    @Test
    public void testWithLongs() throws IOException {
        randomTests(Long.BYTES, Random::nextLong, ByteBuffer::putLong, (output, value) -> {
            try {
                return UpdatableWrite.of(output, value);
            } catch (IOException e) {
                throw new AssertionError(e);        // Should never happen in this test.
            }
        });
    }

    /**
     * Tests one {@link UpdatableWrite} subclass with random data.
     *
     * @param  <V>       type of data.
     * @param  dataSize  data size in number of bytes.
     * @param  next      a getter of next random value.
     * @param  appender  {@link ByteBuffer} method to invoke for adding the value.
     * @param  creator   provider or the {@link UpdatableWrite} instance to test.
     * @throws IOException should never happen since we are writing in memory.
     */
    private <V> void randomTests(final int dataSize, final Function<Random,V> next, final BiConsumer<ByteBuffer,V> appender,
            final BiFunction<ChannelDataOutput, V, UpdatableWrite<V>> creator)
            throws IOException
    {
        final Random random   = TestUtilities.createRandomNumberGenerator();
        final var    expected = ByteBuffer.allocate(128);
        final var    actual   = ByteBuffer.allocate(128);
        final var    output   = new ChannelDataOutput("Test", new ByteArrayChannel(actual.array(), false), ByteBuffer.allocate(24));
        final var    queue    = new ArrayDeque<UpdatableWrite<V>>();
        while (expected.hasRemaining()) {
            // Put some random bytes between the `UpdatableWrite` instances to test.
            for (int i = random.nextInt(Math.min(expected.remaining(), 16)) + 1; --i >= 0;) {
                final int value = random.nextInt();
                expected.put((byte) value);
                output.writeByte(value);
            }
            // Create one `UpdatableWrite` instance initialized to a random value.
            if (expected.remaining() >= dataSize) {
                final V value = next.apply(random);
                appender.accept(expected, value);
                queue.addLast(creator.apply(output, value));        // Instance to test later.
            }
            // Randomly update some previously created instances.
            while (random.nextBoolean()) {
                final UpdatableWrite<V> value = queue.pollFirst();
                if (value == null) break;
                assertTrue(value.tryUpdateBuffer(output));          // Shall be a noop because value didn't changed.
                final V newValue = next.apply(random);
                value.set(newValue);
                if (!value.tryUpdateBuffer(output)) {               // First test.
                    output.mark();
                    value.update(output);                           // Second test.
                    output.reset();
                }
                final int p = expected.position();
                appender.accept(expected.position((int) value.position), newValue);
                expected.position(p);
            }
        }
        output.flush();
        assertArrayEquals(expected.array(), actual.array());
    }
}
