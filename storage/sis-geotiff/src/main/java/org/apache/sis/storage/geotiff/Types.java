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
package org.apache.sis.storage.geotiff;


/**
 * The numerical values of TIFF types.
 *
 * @since   0.8
 * @version 0.8
 * @module
 */
final class Types {
    /**
     * Types defined by the TIFF specification or one of its extensions.
     */
    static final short UBYTE     = 1,
                       ASCII     = 2,
                       USHORT    = 3,
                       UINT      = 4,
                       URATIONAL = 5,      // unsigned Integer / unsigned Integer
                       BYTE      = 6,
                                           // type 7 is undefined
                       SHORT     = 8,
                       INT       = 9,
                       RATIONAL  = 10,     // signed Integer / signed Integer
                       FLOAT     = 11,
                       DOUBLE    = 12,
                       IFD       = 13,     // IFD is like UINT.
                       ULONG     = 16,
                       LONG      = 17,
                       IFD8      = 18;     // IFD is like ULONG.

    /**
     * The size of each type in bytes, or 0 if unknown.
     */
    private static final byte[] SIZE = new byte[19];
    static {
        final byte[] s = SIZE;
        s[ASCII]                      =   Byte.BYTES;
        s[BYTE]      =  s[UBYTE]      =   Byte.BYTES;
        s[SHORT]     =  s[USHORT]     =   Short.BYTES;
        s[INT]       =  s[UINT]       =   Integer.BYTES;
        s[LONG]      =  s[ULONG]      =   Long.BYTES;
        s[RATIONAL]  =  s[URATIONAL]  = 2*Integer.BYTES;
        s[IFD]                        =   Integer.BYTES;
        s[IFD8]                       =   Long.BYTES;
        s[FLOAT]                      =   Float.BYTES;
        s[DOUBLE]                     =   Double.BYTES;
    }

    /**
     * Do not allow instantiation of this class.
     */
    private Types() {
    }

    /**
     * Returns the size in bytes of the given type.
     */
    static int size(final short type) {
        return SIZE[type] & 0xFF;
    }
}
