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
import java.util.OptionalLong;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.CheckedContainer;


/**
 * Placeholder for a primitive value which is written now but can be modified later.
 * A typical usage of this class is when the next value to write in the output stream is the length of something,
 * but that length is not yet known because it depends on how many bytes will be emitted by the next operations.
 * That length may be hard to predict when it is the result of a compression.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <V>  type of value to write.
 */
public abstract class UpdatableWrite<V> implements CheckedContainer<V> {
    /**
     * The stream position where the updatable value has been written.
     * This is the position where the value will be rewritten if it needs to be updated.
     *
     * @see ChannelDataOutput#getStreamPosition()
     */
    public final long position;

    /**
     * Prepares a new updatable value at the current output position.
     *
     * @param  position  stream where to write the value.
     */
    private UpdatableWrite(final ChannelDataOutput output) {
        position = output.getStreamPosition();
    }

    /**
     * Prepares a new updatable value at the specified position.
     *
     * @param  position  position where to write the value.
     */
    private UpdatableWrite(final long position) {
        this.position = position;
    }

    /**
     * Creates a pseudo-updatable associated to no value at the current output position.
     * This variant can be used when the caller only want to record the position, with no write operation.
     *
     * @param  output  stream where to write the value.
     * @param  value   the unsigned short value to write.
     * @return handler for modifying the value later.
     * @throws IOException if an error occurred while writing the value.
     */
    public static UpdatableWrite<Void> of(final ChannelDataOutput output) {
        return new OfVoid(output);
    }

    /**
     * Creates an updatable unsigned short value at the current output position.
     *
     * @param  output  stream where to write the value.
     * @param  value   the unsigned short value to write.
     * @return handler for modifying the value later.
     * @throws IOException if an error occurred while writing the value.
     */
    public static UpdatableWrite<Short> of(final ChannelDataOutput output, final short value) throws IOException {
        final var dw = new OfShort(output, value);
        dw.write(output);
        return dw;
    }

    /**
     * Creates an updatable unsigned integer value at the current output position.
     *
     * @param  output  stream where to write the value.
     * @param  value   the unsigned integer value to write.
     * @return handler for modifying the value later.
     * @throws IOException if an error occurred while writing the value.
     */
    public static UpdatableWrite<Integer> of(final ChannelDataOutput output, final int value) throws IOException {
        final var dw = new OfInt(output, value);
        dw.write(output);
        return dw;
    }

    /**
     * Creates an updatable long value at the current output position.
     *
     * @param  output  stream where to write the value.
     * @param  value   the value to write.
     * @return handler for modifying the value later.
     * @throws IOException if an error occurred while writing the value.
     */
    public static UpdatableWrite<Long> of(final ChannelDataOutput output, final long value) throws IOException {
        final var dw = new OfLong(output, value);
        dw.write(output);
        return dw;
    }

    /**
     * Creates an updatable value at the specified position.
     * The existing value is assumed to be zero.
     *
     * @param  <V>       compile-time value of {@code type}.
     * @param  position  position where the value is written. Current value must be zero (this is not verified).
     * @param  type      class of the value as {@code Short.class}, {@code Integer.class} or {@code Long.class}.
     * @return handler for modifying the value later.
     */
    @SuppressWarnings("unchecked")
    public static <V extends Number> UpdatableWrite<V> ofZeroAt(final long position, final Class<V> type) {
        if (type == Integer.class) return (UpdatableWrite<V>) new OfInt  (position);
        if (type ==   Short.class) return (UpdatableWrite<V>) new OfShort(position);
        if (type ==    Long.class) return (UpdatableWrite<V>) new OfLong (position);
        throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "type", type));
    }

    /**
     * Implementation of {@link UpdatableWrite#of(ChannelDataOutput)}.
     * This class only records the stream position, with no associated value.
     */
    private static final class OfVoid extends UpdatableWrite<Void> {
        OfVoid(ChannelDataOutput output)                 {super(output);}
        @Override public Class<Void>  getElementType()   {return Void.class;}
        @Override public int          sizeInBytes()      {return 0;}
        @Override public boolean      changed()          {return false;}
        @Override public Void         get()              {return null;}
        @Override public OptionalLong getAsLong()        {return OptionalLong.empty();}
        @Override public void         set(Void v)        {}
        @Override public void         setAsLong(long v)  {}
        @Override void put(ByteBuffer output, int index) {}
        @Override void write(ChannelDataOutput output)   {}
    }

    /**
     * Implementation of {@link UpdatableWrite#of(ChannelDataOutput, short)}.
     * The value is stored as an unsigned 16-bits integer.
     */
    private static final class OfShort extends UpdatableWrite<Short> {
        private short current, defined;

        OfShort(final ChannelDataOutput output, final short value) {
            super(output);
            current = value;
            defined = value;
        }

        OfShort(long position) {super(position);}
        @Override public Class<Short> getElementType()   {return Short.class;}
        @Override public int          sizeInBytes()      {return Short.BYTES;}
        @Override public boolean      changed()          {return defined != current;}
        @Override public Short        get()              {return defined;}
        @Override public OptionalLong getAsLong()        {return OptionalLong.of(Short.toUnsignedLong(defined));}
        @Override public void         set(Short v)       {defined = v;}
        @Override public void         setAsLong(long v)  {defined = (short) validate(Short.SIZE, v);}
        @Override void put(ByteBuffer output, int index) {
            output.putShort(index, current = defined);
        }
        @Override void write(ChannelDataOutput output) throws IOException {
            output.writeShort(current = defined);
        }
    }

    /**
     * Implementation of {@link UpdatableWrite#of(ChannelDataOutput, int)}.
     * The value is stored as an unsigned 32-bits integer.
     */
    private static final class OfInt extends UpdatableWrite<Integer> {
        private int current, defined;

        OfInt(final ChannelDataOutput output, final int value) {
            super(output);
            current = value;
            defined = value;
        }

        OfInt(long position) {super(position);}
        @Override public Class<Integer> getElementType()  {return Integer.class;}
        @Override public int            sizeInBytes()     {return Integer.BYTES;}
        @Override public boolean        changed()         {return defined != current;}
        @Override public Integer        get()             {return defined;}
        @Override public OptionalLong   getAsLong()       {return OptionalLong.of(Integer.toUnsignedLong(defined));}
        @Override public void           set(Integer v)    {defined = v;}
        @Override public void           setAsLong(long v) {defined = (int) validate(Integer.SIZE, v);}
        @Override void put(ByteBuffer output, int index)  {
            output.putInt(index, current = defined);
        }
        @Override void write(ChannelDataOutput output) throws IOException {
            output.writeInt(current = defined);
        }
    }

    /**
     * Implementation of {@link UpdatableWrite#of(ChannelDataOutput, long)}.
     * The value is stored as a 64-bits integer.
     */
    private static final class OfLong extends UpdatableWrite<Long> {
        private long current, defined;

        OfLong(final ChannelDataOutput output, final long value) {
            super(output);
            current = value;
            defined = value;
        }

        OfLong(long position) {super(position);}
        @Override public Class<Long>  getElementType()   {return Long.class;}
        @Override public int          sizeInBytes()      {return Long.BYTES;}
        @Override public boolean      changed()          {return defined != current;}
        @Override public Long         get()              {return defined;}
        @Override public OptionalLong getAsLong()        {return OptionalLong.of(defined);}
        @Override public void         set(Long v )       {defined = v;}
        @Override public void         setAsLong(long v)  {defined = v;}
        @Override void put(ByteBuffer output, int index) {
            output.putLong(index, current = defined);
        }
        @Override void write(ChannelDataOutput output) throws IOException {
            output.writeLong(current = defined);
        }
    }

    /**
     * Returns the value size in number of bytes.
     * This is used for checking if there is enough room in a buffer.
     */
    abstract int sizeInBytes();

    /**
     * Returns whether the value changed since it has been written.
     * This is used for avoiding unnecessary write operations in the output.
     */
    abstract boolean changed();

    /**
     * Returns the value to write.
     * This is the value specified in the last call to {@link #set(V)},
     * or the value specified at construction time if {@code set(V)} has never been invoked.
     * May be null if and only if the {@linkplain #getElementType() element type} is {@link Void}.
     *
     * @return the value to write, or {@code null} if none.
     */
    public abstract V get();

    /**
     * Sets the value to write. The value is not written immediately.
     * Value will become effective if {@link #tryUpdateBuffer(ChannelDataOutput)}
     * returns {@code true}, or when {@link #update(ChannelDataOutput)} is invoked.
     *
     * @param  value  the value to write.
     */
    public abstract void set(V value);

    /**
     * Returns the same value as {@code get()}, but as a 64-bits integer.
     * This method is useful when the parameterized type is {@code <?>}.
     */
    public abstract OptionalLong getAsLong();

    /**
     * Sets the value as a 64-bits integer.
     * This method does the same work as {@link #set(V)}.
     * It is useful when the parameterized type is {@code <?>}.
     *
     * @param  value  the value to write, to be interpreted as an unsigned integer.
     * @throws ArithmeticException if the given value overflows the capacity of this handler.
     */
    public abstract void setAsLong(long value);

    /**
     * Verifies if the given value can be cast to an unsigned number of the given number of bits.
     *
     * @param  size   number of bits of the destination.
     * @param  value  the value to validate.
     * @return the given value.
     * @throws ArithmeticException if the given value overflows the capacity of the caller.
     */
    static long validate(final int size, final long value) {
        if ((value & ~((1L << size) - 1)) != 0) {
            throw new ArithmeticException(Errors.format(Errors.Keys.IntegerOverflow_1, size));
        }
        return value;
    }

    /**
     * Updates the buffer at the given index with the value of this handler.
     *
     * @param output  the buffer to update.
     * @param index   index of the value to update.
     */
    abstract void put(ByteBuffer output, int index);

    /**
     * Writes the value in the specified output.
     * The stream position shall be set by the caller.
     *
     * @param  output  the output stream to update.
     * @throws IOException if an error occurred while writing to the output.
     */
    abstract void write(ChannelDataOutput output) throws IOException;

    /**
     * Tries to write the deferred value by updating the buffer if possible, otherwise does nothing.
     * If the buffer has not yet been written to the output channel, updating that buffer before its
     * use is more efficient. It that case, there is no need to remember this {@code UpdatableWrite}
     * instance for later update of the output stream.
     *
     * @param  output  the output stream to update.
     * @return whether the deferred value has been written.
     */
    public final boolean tryUpdateBuffer(final ChannelDataOutput output) {
        if (changed()) {
            final long p = Math.subtractExact(position, output.bufferOffset);
            if (!(p >= 0 && p <= output.buffer.position() - sizeInBytes())) {
                return false;
            }
            put(output.buffer, (int) p);
        }
        return true;
    }

    /**
     * Writes the updated value in the specified output stream.
     * It is caller's responsibility to mark and reset the stream position
     * before to invoke this method if desired.
     *
     * @param  output  the output stream to update.
     * @throws IOException if an error occurred while writing to the output.
     */
    public final void update(final ChannelDataOutput output) throws IOException {
        if (changed()) {
            output.seek(position);
            write(output);
        }
    }
}
