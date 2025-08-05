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
package org.apache.sis.image;

import java.nio.Buffer;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.io.InvalidObjectException;
import java.awt.image.Raster;
import java.awt.image.DataBuffer;
import org.apache.sis.util.resources.Errors;


/**
 * The type of data used to transfer pixels. Data transfers happen in various {@link Raster} methods and in
 * {@link PixelIterator#createWindow(TransferType)}. The type used for transferring data is not necessarily
 * the same as the type used by the raster for storing data. In particular, {@code byte} and {@code short}
 * (both signed and unsigned) are converted to {@code int} during the transfer.
 *
 * <p>{@link Raster} and {@link PixelIterator} transfer data in {@code int[]}, {@code float[]} and {@code double[]} arrays.
 * Additionally, {@code PixelIterator} uses also {@link IntBuffer}, {@link FloatBuffer} and {@link DoubleBuffer}.</p>
 *
 * <h2>Future evolution</h2>
 * This class may be refactored as an enumeration in a future Java version if
 * <a href="http://openjdk.java.net/jeps/301">JEP 301</a> is implemented.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @param  <T>  the type of buffer which can be used for transferring data.
 *
 * @since 1.0
 *
 * @see Raster#getTransferType()
 * @see PixelIterator#createWindow(TransferType)
 */
public final class TransferType<T extends Buffer> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2891665589742927570L;

    /**
     * The enumeration name.
     */
    private final transient String name;

    /**
     * The type as one of the {@link DataBuffer} constants.
     * This is the value returned by {@link Raster#getTransferType()}.
     */
    final int dataBufferType;

    /**
     * Specifies that sample values are transferred as 32 bits signed integer.
     * If the raster stores sample values as {@code byte} or {@code short}, the values are cast by a widening
     * conversion before to be transferred. If the raster stores sample values as {@code float} or {@code double},
     * the values are rounded toward 0 before to be transferred.
     *
     * @see PixelIterator#getSample(int)
     * @see PixelIterator#getPixel(int[])
     */
    public static final TransferType<IntBuffer> INT = new TransferType<>("INT", DataBuffer.TYPE_INT);

    /**
     * Specifies that sample values are transferred as single-precision floating point number.
     * Values of other types are cast as needed.
     *
     * @see PixelIterator#getSampleFloat(int)
     * @see PixelIterator#getPixel(float[])
     */
    public static final TransferType<FloatBuffer> FLOAT = new TransferType<>("FLOAT", DataBuffer.TYPE_FLOAT);

    /**
     * Specifies that sample values are transferred as double-precision floating point number.
     * Values of other types are cast as needed. This is the safest transfer type to use
     * when wanting to avoid any precision lost.
     *
     * @see PixelIterator#getSampleDouble(int)
     * @see PixelIterator#getPixel(double[])
     */
    public static final TransferType<DoubleBuffer> DOUBLE = new TransferType<>("DOUBLE", DataBuffer.TYPE_DOUBLE);

    /**
     * Creates a new enumeration.
     */
    private TransferType(final String name, final int dataBufferType) {
        this.name = name;
        this.dataBufferType = dataBufferType;
    }

    /**
     * Returns the enumeration value for the given {@code DataBuffer} constant.
     * This method applies the following mapping:
     *
     * <ul>
     *   <li>If {@code type} is {@link DataBuffer#TYPE_DOUBLE}, returns {@link #DOUBLE}.</li>
     *   <li>If {@code type} is {@link DataBuffer#TYPE_FLOAT}, returns {@link #FLOAT}.</li>
     *   <li>If {@code type} is {@link DataBuffer#TYPE_INT}, {@link DataBuffer#TYPE_SHORT TYPE_SHORT},
     *          {@link DataBuffer#TYPE_USHORT TYPE_USHORT} or {@link DataBuffer#TYPE_BYTE TYPE_BYTE},
     *          returns {@link #INT}.</li>
     *   <li>If {@code type} is {@link DataBuffer#TYPE_UNDEFINED} or any other value,
     *       throws {@link IllegalArgumentException}.</li>
     * </ul>
     *
     * The {@code type} argument given to this method is typically the {@link Raster#getTransferType()} value.
     *
     * @param  type  one of {@link DataBuffer} constant.
     * @return the enumeration value for the given constant.
     * @throws IllegalArgumentException if (@code type} is not a supported {@code DataBuffer} constant.
     */
    public static TransferType<?> valueOf(final int type) {
        switch (type) {
            case DataBuffer.TYPE_DOUBLE: return DOUBLE;
            case DataBuffer.TYPE_FLOAT:  return FLOAT;
            default: {
                if (type >= DataBuffer.TYPE_BYTE && type <= DataBuffer.TYPE_INT) return INT;
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "type", type));
            }
        }
    }

    /**
     * Returns a unique instance on deserialization.
     *
     * @return the object to use after deserialization.
     * @throws ObjectStreamException if the serialized object defines an unknown data type.
     */
    Object readResolve() throws ObjectStreamException {
        try {
            return valueOf(dataBufferType);
        } catch (IllegalArgumentException e) {
            throw new InvalidObjectException(e.toString());
        }
    }

    /**
     * Returns the name of this enumeration constant.
     *
     * @return the enumeration constant name.
     */
    @Override
    public String toString() {
        return name;
    }
}
