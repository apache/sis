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
package org.apache.sis.storage.gsf;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.StructLayout;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class StructClass {

    protected final MemorySegment struct;

    public StructClass(MemorySegment struct) {
        this.struct = struct;
    }

    public StructClass(SegmentAllocator allocator) {
        this.struct = allocator.allocate(getLayout());
    }

    protected abstract MemoryLayout getLayout();

    public MemorySegment getMemorySegment() {
        return struct;
    }

    @Override
    public String toString() {
        final Class<? extends StructClass> clazz = getClass();
        final String name = clazz.getSimpleName();
        final List<String> attributes = new ArrayList<>();

        final MemoryLayout layout = getLayout();
        if (layout instanceof StructLayout sl) {
            for(MemoryLayout ml : sl.memberLayouts()) {
                final String attName = ml.name().orElse(null);
                if (attName != null) {
                    String result;
                    try {
                        Object value = clazz.getMethod("get" + toCamelCase(attName)).invoke(this);
                        if (value != null && value.getClass().isArray()) {
                            //pick the first 10 values
                            final int length = Array.getLength(value);
                            final StringBuilder sb = new StringBuilder("[");
                            for (int i = 0; i < 10 && i < length; i++) {
                                sb.append(Array.get(value, i));
                                sb.append(',');
                            }
                            sb.append("...]");
                            value = sb.toString();
                        }
                        result = String.valueOf(value);
                    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException ex) {
                        result = "[Failed to get value]";
                    }
                    attributes.add(attName + " : " + result);
                }
            }
        }

        return toStringTree(name, attributes);
    }

    private static String toCamelCase(String str) {
        final StringBuilder sb = new StringBuilder();
        for (String s : str.split("_")) {
            if (!s.isEmpty()) {
                sb.append(Character.toUpperCase(s.charAt(0)));
                sb.append(s.substring(1));
            }
        }
        return sb.toString();
    }

    protected byte[] getBytes(int offset, int nb) {
        return getBytes(struct, offset, nb);
    }

    protected static byte[] getBytes(MemorySegment segment, int offset, int nb) {
        if (segment.address() == 0L) return null; //C null adress
        final ByteBuffer db = segment.asSlice(offset, nb).asByteBuffer();
        final byte[] dst = new byte[nb];
        db.get(0, dst);
        return dst;
    }

    protected short[] getShorts(int offset, int nb) {
        return getShorts(struct, offset, nb);
    }

    protected static short[] getShorts(MemorySegment segment, int offset, int nb) {
        if (segment.address() == 0L) return null; //C null adress
        final ShortBuffer db = segment.asSlice(offset, nb*2).asByteBuffer().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        final short[] dst = new short[nb];
        db.get(0, dst);
        return dst;
    }

    protected int[] getInts(int offset, int nb) {
        return getInts(struct, offset, nb);
    }

    protected static int[] getInts(MemorySegment segment, int offset, int nb) {
        if (segment.address() == 0L) return null; //C null adress
        final IntBuffer db = segment.asSlice(offset, nb*2).asByteBuffer().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        final int[] dst = new int[nb];
        db.get(0, dst);
        return dst;
    }

    protected long[] getLongs(int offset, int nb) {
        return getLongs(struct, offset, nb);
    }

    protected static long[] getLongs(MemorySegment segment, int offset, int nb) {
        if (segment.address() == 0L) return null; //C null adress
        final LongBuffer db = segment.asSlice(offset, nb*2).asByteBuffer().order(ByteOrder.LITTLE_ENDIAN).asLongBuffer();
        final long[] dst = new long[nb];
        db.get(0, dst);
        return dst;
    }

    protected double[] getDoubles(int offset, int nb) {
        return getDoubles(struct, offset, nb);
    }

    protected static double[] getDoubles(MemorySegment segment, int offset, int nb) {
        if (segment.address() == 0L) return null; //C null adress
        final DoubleBuffer db = segment.asSlice(offset, nb*8).asByteBuffer().order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer();
        final double[] dst = new double[nb];
        db.get(0, dst);
        return dst;
    }

    protected <T extends StructClass> T[] getObjects(int offset, int nb, Class<T> clazz) {
        try {
            final Constructor<T> constructor = clazz.getConstructor(MemorySegment.class);
            final GroupLayout layout = (GroupLayout) clazz.getDeclaredField("LAYOUT").get(null);
            final long byteSize = layout.byteSize();

            final T[] table = (T[]) Array.newInstance(clazz, nb);
            int off = offset;
            for (int i = 0; i < nb; i++, off += byteSize) {
                final MemorySegment segment = struct.asSlice(off, byteSize);
                table[i] = constructor.newInstance(segment);
            }
            return table;
        } catch (InstantiationException | NoSuchMethodException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Incorrect Struct class declaration", ex);
        }
    }

    /**
     * Returns a graphical representation of the specified objects. This representation can be
     * printed to the {@linkplain System#out standard output stream} (for example) if it uses
     * a monospaced font and supports unicode.
     *
     * @param  root  The root name of the tree to format.
     * @param  objects The objects to format as root children.
     * @return A string representation of the tree.
     */
    protected static String toStringTree(String root, final Iterable<?> objects) {
        final StringBuilder sb = new StringBuilder();
        if (root != null) {
            sb.append(root);
        }
        if (objects != null) {
            final Iterator<?> ite = objects.iterator();
            while (ite.hasNext()) {
                sb.append('\n');
                final Object next = ite.next();
                final boolean last = !ite.hasNext();
                sb.append(last ? "\u2514\u2500 " : "\u251C\u2500 ");

                final String[] parts = String.valueOf(next).split("\n");
                sb.append(parts[0]);
                for (int k=1;k<parts.length;k++) {
                    sb.append('\n');
                    sb.append(last ? ' ' : '\u2502');
                    sb.append("  ");
                    sb.append(parts[k]);
                }
            }
        }
        return sb.toString();
    }
}
