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
package org.apache.sis.internal.storage;

import java.util.Arrays;
import java.io.IOException;
import java.nio.ByteOrder;
import javax.imageio.stream.IIOByteBuffer;
import javax.imageio.stream.ImageInputStream;

import static org.junit.Assert.*;


/**
 * Compares the result of two {@link ImageInputStream} instances. This class is used for comparing
 * {@link ChannelImageInputStream} with the JDK implementation, where the JDK's one is used as the
 * reference implementation.
 *
 * <p><b>This class is provided for debugging purpose only.</b> This class can not be used in test
 * suite because it checks for identical behavior between the two input streams, which is usually
 * a too strong requirement. For example two streams may read a different amount of bytes in a call
 * to {@link #read(byte[])} and still be compliant with their contract.</p>
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public class ImageInputStreamComparator implements ImageInputStream {
    /**
     * The reference implementation ("expected") and the tested ("actual") input streams.
     */
    private final ImageInputStream expected, actual;

    /**
     * Creates a new comparator for the given input streams.
     *
     * @param expected The stream used as a reference implementation.
     * @param actual   The stream to compare against the reference implementation.
     */
    public ImageInputStreamComparator(final ImageInputStream expected, final ImageInputStream actual) {
        this.expected = expected;
        this.actual   = actual;
    }

    /**
     * Forwards the call to the two streams.
     *
     * @param byteOrder The byte order to set.
     */
    @Override
    public void setByteOrder(final ByteOrder byteOrder) {
        expected.setByteOrder(byteOrder);
        actual  .setByteOrder(byteOrder);
    }

    /**
     * Forwards the call to the two streams and ensures that they return identical results.
     *
     * @return The result of the forwarded call.
     */
    @Override
    public ByteOrder getByteOrder() {
        final ByteOrder r = expected.getByteOrder();
        assertEquals(r, actual.getByteOrder());
        return r;
    }

    /**
     * Forwards the call to the two streams and ensures that they return identical results.
     *
     * @return The result of the forwarded call.
     * @throws IOException if any of the two streams failed to perform the operation.
     */
    @Override
    public int read() throws IOException {
        final int r = expected.read();
        assertEquals(r, actual.read());
        return r;
    }

    /**
     * Forwards the call to the two streams and ensures that they return identical results,
     * <strong>without tolerance for normally allowed differences</strong>.
     * This method requires that the two streams read an identical amount of bytes,
     * despite the method contract allowing a different amount of bytes to be read.
     *
     * @param  dest The destination array where to store the bytes read.
     * @return The result of the forwarded call.
     * @throws IOException if any of the two streams failed to perform the operation.
     */
    @Override
    public int read(final byte[] dest) throws IOException {
        final byte[] copy = dest.clone();
        final int r = expected.read(dest);
        assertEquals(r, actual.read(copy));
        assertArrayEquals(dest, copy);
        return r;
    }

    @Override
    public int read(final byte[] dest, final int offset, final int length) throws IOException {
        final byte[] copy = dest.clone();
        final int r = expected.read(dest, offset, length);
        assertEquals(r, actual.read(copy, offset, length));
        final boolean subRange = (offset != 0) || (length != dest.length);
        assertArrayEquals(subRange ? Arrays.copyOfRange(dest, offset, offset + length) : dest,
                          subRange ? Arrays.copyOfRange(copy, offset, offset + length) : copy);
        return r;
    }

    @Override
    public void readBytes(final IIOByteBuffer dest, final int n) throws IOException {
        final IIOByteBuffer copy = new IIOByteBuffer(dest.getData().clone(), dest.getOffset(), dest.getLength());
        expected.readBytes(dest, n);
        actual  .readBytes(copy,   n);
        final int offset = dest.getOffset();
        final int length = dest.getLength();
        assertEquals("offset", offset, copy.getOffset());
        assertEquals("length", length, copy.getLength());
        assertArrayEquals(Arrays.copyOfRange(dest.getData(), offset, offset + length),
                          Arrays.copyOfRange(copy.getData(), offset, offset + length));
    }

    @Override
    public boolean readBoolean() throws IOException {
        final boolean r = expected.readBoolean();
        assertEquals(r, actual.readBoolean());
        return r;
    }

    @Override
    public byte readByte() throws IOException {
        final byte r = expected.readByte();
        assertEquals(r, actual.readByte());
        return r;
    }

    @Override
    public int readUnsignedByte() throws IOException {
        final int r = expected.readUnsignedByte();
        assertEquals(r, actual.readUnsignedByte());
        return r;
    }

    @Override
    public short readShort() throws IOException {
        final short r = expected.readShort();
        assertEquals(r, actual.readShort());
        return r;
    }

    @Override
    public int readUnsignedShort() throws IOException {
        final int r = expected.readUnsignedShort();
        assertEquals(r, actual.readUnsignedShort());
        return r;
    }

    @Override
    public char readChar() throws IOException {
        final char r = expected.readChar();
        assertEquals(r, actual.readChar());
        return r;
    }

    @Override
    public int readInt() throws IOException {
        final int r = expected.readInt();
        assertEquals(r, actual.readInt());
        return r;
    }

    @Override
    public long readUnsignedInt() throws IOException {
        final long r = expected.readUnsignedInt();
        assertEquals(r, actual.readUnsignedInt());
        return r;
    }

    @Override
    public long readLong() throws IOException {
        final long r = expected.readLong();
        assertEquals(r, actual.readLong());
        return r;
    }

    @Override
    public float readFloat() throws IOException {
        final float r = expected.readFloat();
        assertEquals(r, actual.readFloat(), 0f);
        return r;
    }

    @Override
    public double readDouble() throws IOException {
        final double r = expected.readDouble();
        assertEquals(r, actual.readDouble(), 0d);
        return r;
    }

    @Override
    public String readLine() throws IOException {
        final String r = expected.readLine();
        assertEquals(r, actual.readLine());
        return r;
    }

    @Override
    public String readUTF() throws IOException {
        final String r = expected.readUTF();
        assertEquals(r, actual.readUTF());
        return r;
    }

    @Override
    public void readFully(final byte[] dest) throws IOException {
        final byte[] copy = dest.clone();
        expected.readFully(dest);
        actual.readFully(copy);
        assertArrayEquals(dest, copy);
    }

    @Override
    public void readFully(final byte[] dest, final int offset, final int length) throws IOException {
        final byte[] copy = dest.clone();
        expected.readFully(dest, offset, length);
        actual  .readFully(copy, offset, length);
        final boolean subRange = (offset != 0) || (length != dest.length);
        assertArrayEquals(subRange ? Arrays.copyOfRange(dest, offset, offset + length) : dest,
                          subRange ? Arrays.copyOfRange(copy, offset, offset + length) : copy);
    }

    @Override
    public void readFully(final short[] dest, final int offset, final int length) throws IOException {
        final short[] copy = dest.clone();
        expected.readFully(dest, offset, length);
        actual  .readFully(copy, offset, length);
        final boolean subRange = (offset != 0) || (length != dest.length);
        assertArrayEquals(subRange ? Arrays.copyOfRange(dest, offset, offset + length) : dest,
                          subRange ? Arrays.copyOfRange(copy, offset, offset + length) : copy);
    }

    @Override
    public void readFully(final char[] dest, final int offset, final int length) throws IOException {
        final char[] copy = dest.clone();
        expected.readFully(dest, offset, length);
        actual  .readFully(copy, offset, length);
        final boolean subRange = (offset != 0) || (length != dest.length);
        assertArrayEquals(subRange ? Arrays.copyOfRange(dest, offset, offset + length) : dest,
                          subRange ? Arrays.copyOfRange(copy, offset, offset + length) : copy);
    }

    @Override
    public void readFully(final int[] dest, final int offset, final int length) throws IOException {
        final int[] copy = dest.clone();
        expected.readFully(dest, offset, length);
        actual  .readFully(copy, offset, length);
        final boolean subRange = (offset != 0) || (length != dest.length);
        assertArrayEquals(subRange ? Arrays.copyOfRange(dest, offset, offset + length) : dest,
                          subRange ? Arrays.copyOfRange(copy, offset, offset + length) : copy);
    }

    @Override
    public void readFully(final long[] dest, final int offset, final int length) throws IOException {
        final long[] copy = dest.clone();
        expected.readFully(dest, offset, length);
        actual  .readFully(copy, offset, length);
        final boolean subRange = (offset != 0) || (length != dest.length);
        assertArrayEquals(subRange ? Arrays.copyOfRange(dest, offset, offset + length) : dest,
                          subRange ? Arrays.copyOfRange(copy, offset, offset + length) : copy);
    }

    @Override
    public void readFully(final float[] dest, final int offset, final int length) throws IOException {
        final float[] copy = dest.clone();
        expected.readFully(dest, offset, length);
        actual  .readFully(copy, offset, length);
        final boolean subRange = (offset != 0) || (length != dest.length);
        assertArrayEquals(subRange ? Arrays.copyOfRange(dest, offset, offset + length) : dest,
                          subRange ? Arrays.copyOfRange(copy, offset, offset + length) : copy, 0f);
    }

    @Override
    public void readFully(final double[] dest, final int offset, final int length) throws IOException {
        final double[] copy = dest.clone();
        expected.readFully(dest, offset, length);
        actual  .readFully(copy, offset, length);
        final boolean subRange = (offset != 0) || (length != dest.length);
        assertArrayEquals(subRange ? Arrays.copyOfRange(dest, offset, offset + length) : dest,
                          subRange ? Arrays.copyOfRange(copy, offset, offset + length) : copy, 0d);
    }

    @Override
    public long getStreamPosition() throws IOException {
        final long r = expected.getStreamPosition();
        assertEquals(r, actual.getStreamPosition());
        return r;
    }

    @Override
    public int getBitOffset() throws IOException {
        final int r = expected.getBitOffset();
        assertEquals(r, actual.getBitOffset());
        return r;
    }

    @Override
    public void setBitOffset(int bitOffset) throws IOException {
        expected.setBitOffset(bitOffset);
        actual  .setBitOffset(bitOffset);
    }

    @Override
    public int readBit() throws IOException {
        final int r = expected.readBit();
        assertEquals(r, actual.readBit());
        return r;
    }

    @Override
    public long readBits(final int numBits) throws IOException {
        final long r = expected.readBits(numBits);
        assertEquals(r, actual.readBits(numBits));
        return r;
    }

    @Override
    public long length() throws IOException {
        final long r = expected.length();
        assertEquals(r, actual.length());
        return r;
    }

    @Override
    public int skipBytes(final int n) throws IOException {
        final int r = expected.skipBytes(n);
        assertEquals(r, actual.skipBytes(n));
        return r;
    }

    @Override
    public long skipBytes(long n) throws IOException {
        final long r = expected.skipBytes(n);
        assertEquals(r, actual.skipBytes(n));
        return r;
    }

    @Override
    public void seek(long pos) throws IOException {
        expected.seek(pos);
        actual  .seek(pos);
    }

    @Override
    public void mark() {
        expected.mark();
        actual  .mark();
    }

    @Override
    public void reset() throws IOException {
        expected.reset();
        actual  .reset();
    }

    @Override
    public void flushBefore(final long position) throws IOException {
        expected.flushBefore(position);
        actual  .flushBefore(position);
    }

    @Override
    public void flush() throws IOException {
        expected.flush();
        actual  .flush();
    }

    @Override
    public long getFlushedPosition() {
        final long r = expected.getFlushedPosition();
        assertEquals(r, actual.getFlushedPosition());
        return r;
    }

    @Override
    public boolean isCached() {
        final boolean r = expected.isCached();
        assertEquals(r, actual.isCached());
        return r;
    }

    @Override
    public boolean isCachedMemory() {
        final boolean r = expected.isCachedMemory();
        assertEquals(r, actual.isCachedMemory());
        return r;
    }

    @Override
    public boolean isCachedFile() {
        final boolean r =  expected.isCachedFile();
        assertEquals(r, actual.isCachedFile());
        return r;
    }

    @Override
    public void close() throws IOException {
        expected.close();
        actual  .close();
    }
}
