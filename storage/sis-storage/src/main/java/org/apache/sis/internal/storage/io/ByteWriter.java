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
package org.apache.sis.internal.storage.io;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import org.apache.sis.math.Vector;
import org.apache.sis.internal.jdk9.JDK9;


/**
 * Copies bytes from a source {@link Buffer} of arbitrary kind to a target {@link ByteBuffer}.
 * This class can be used when the source {@link Buffer} subclass is unknown at compile-time.
 * If the source buffer has a greater capacity than the target buffer, then {@link #write()}
 * can be invoked in a loop until all data have been transferred.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public abstract class ByteWriter {
    /**
     * For subclass constructors.
     */
    private ByteWriter() {
    }

    /**
     * Creates a new writer for copying bytes from the given source vector to the given target array.
     * This is a convenience method delegating to {@link #create(Buffer, ByteBuffer)}.
     *
     * @param  source  the vector from which to copy data.
     * @param  target  the array where to copy data.
     * @return a writer from given source to target.
     */
    public static ByteWriter create(final Vector source, final byte[] target) {
        return create(source.buffer().orElseGet(() -> DoubleBuffer.wrap(source.doubleValues())),
                      ByteBuffer.wrap(target).order(ByteOrder.nativeOrder()));
    }

    /**
     * Creates a new writer for copying bytes from the given source to the given target buffers.
     * Data will be read from the current position of source buffer up to that buffer <em>limit</em>.
     * Data will be written starting at position 0 of target buffer up to that buffer <em>capacity</em>.
     * The position and limit of target buffer are ignored.
     * The position and limit of both buffers may be modified by this method.
     *
     * @param  source  the buffer from which to copy data.
     * @param  target  the buffer where to copy data.
     * @return a writer from given source to target.
     */
    public static ByteWriter create(Buffer source, final ByteBuffer target) {
        if (source.limit() != source.capacity()) {
            source = JDK9.slice(source);
        }
        if (source instanceof DoubleBuffer) return new Doubles ((DoubleBuffer) source, target);
        if (source instanceof  FloatBuffer) return new Floats  ( (FloatBuffer) source, target);
        if (source instanceof   LongBuffer) return new Longs   (  (LongBuffer) source, target);
        if (source instanceof    IntBuffer) return new Integers(   (IntBuffer) source, target);
        if (source instanceof  ShortBuffer) return new Shorts  ( (ShortBuffer) source, target);
        if (source instanceof   ByteBuffer) return new Bytes   (  (ByteBuffer) source, target);
        throw new IllegalArgumentException();
    }

    /**
     * Copies bytes from the source buffer to the target buffer. The target buffer position is not overwritten;
     * it is caller responsibility to update it (if desired) from the value returned by this method.
     *
     * @return the number of bytes copied, or 0 if done.
     */
    public abstract int write();

    /**
     * Prepares the given source and target buffers to a new transfer.
     */
    private static void reset(final Buffer source, final Buffer target) {
        target.clear();
        source.limit(Math.min(source.capacity(), source.position() + target.capacity()));
    }

    /** Writer for {@code double} values. */
    private static final class Doubles extends ByteWriter {
        /** The buffers between which to transfer data. */
        private final DoubleBuffer source, target;

        /** Creates a new writer for the given source. */
        Doubles(final DoubleBuffer source, final ByteBuffer target) {
            this.source = source;
            this.target = target.asDoubleBuffer();
        }

        /** Write bytes. */
        @Override public int write() {
            reset(source, target);
            target.put(source);
            return target.position() * Double.BYTES;
        }
    }

    /** Writer for {@code float} values. */
    private static final class Floats extends ByteWriter {
        /** The buffers between which to transfer data. */
        private final FloatBuffer source, target;

        /** Creates a new writer for the given source. */
        Floats(final FloatBuffer source, final ByteBuffer target) {
            this.source = source;
            this.target = target.asFloatBuffer();
        }

        /** Write bytes. */
        @Override public int write() {
            reset(source, target);
            target.put(source);
            return target.position() * Float.BYTES;
        }
    }

    /** Writer for {@code long} values. */
    private static final class Longs extends ByteWriter {
        /** The buffers between which to transfer data. */
        private final LongBuffer source, target;

        /** Creates a new writer for the given source. */
        Longs(final LongBuffer source, final ByteBuffer target) {
            this.source = source;
            this.target = target.asLongBuffer();
        }

        /** Write bytes. */
        @Override public int write() {
            reset(source, target);
            target.put(source);
            return target.position() * Long.BYTES;
        }
    }

    /** Writer for {@code int} values. */
    private static final class Integers extends ByteWriter {
        /** The buffers between which to transfer data. */
        private final IntBuffer source, target;

        /** Creates a new writer for the given source. */
        Integers(final IntBuffer source, final ByteBuffer target) {
            this.source = source;
            this.target = target.asIntBuffer();
        }

        /** Write bytes. */
        @Override public int write() {
            reset(source, target);
            target.put(source);
            return target.position() * Integer.BYTES;
        }
    }

    /** Writer for {@code short} values. */
    private static final class Shorts extends ByteWriter {
        /** The buffers between which to transfer data. */
        private final ShortBuffer source, target;

        /** Creates a new writer for the given source. */
        Shorts(final ShortBuffer source, final ByteBuffer target) {
            this.source = source;
            this.target = target.asShortBuffer();
        }

        /** Write bytes. */
        @Override public int write() {
            reset(source, target);
            target.put(source);
            return target.position() * Short.BYTES;
        }
    }

    /** Writer for {@code byte} values. */
    private static final class Bytes extends ByteWriter {
        /** The buffers between which to transfer data. */
        private final ByteBuffer source, target;

        /** Creates a new writer for the given source. */
        Bytes(final ByteBuffer source, final ByteBuffer target) {
            this.source = source;
            this.target = target;
        }

        /** Write bytes. */
        @Override public int write() {
            reset(source, target);
            target.put(source);
            return target.position() * Byte.BYTES;
        }
    }
}
