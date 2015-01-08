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
package org.apache.sis.internal.shapefile.jdbc;

import java.nio.MappedByteBuffer;

import org.apache.sis.util.logging.AutoChecker;


/**
 * Field descriptor.
 *
 * @author  Travis L. Pinney
 * @since   0.5
 * @version 0.5
 * @module
 */
public class DBase3FieldDescriptor extends AutoChecker {
    /** Field name. */
    private byte[] fieldName = new byte[11];

    /** Field type. */
    private DBaseDataType fieldType;

    /** Field address (Field data address (address is set in memory; not useful on disk). */
    private byte[] fieldAddress = new byte[4];

    /** Field length. */
    private byte fieldLength;

    /** Decimal count. */
    private byte fieldDecimalCount;

    /** Reserved 2. */
    private byte[] dbasePlusLanReserved2 = new byte[2];

    /** Work area id. */
    @SuppressWarnings("unused") // Kept in case of later ALTER TABLE sql calls.
    private byte workAreaID;

    /** Reserved 3. */
    private byte[] dbasePlusLanReserved3 = new byte[2];

    /** Undocumented use. */
    @SuppressWarnings("unused") // Kept in case of later ALTER TABLE sql calls.
    private byte setFields;

    /**
     * Create a field descriptor from the current position of the binary stream.
     * @param byteBuffer ByteBuffer.
     */
    public DBase3FieldDescriptor(MappedByteBuffer byteBuffer) {
        // Field name.
        byteBuffer.get(fieldName);

        // Field type.
        char dt = (char)byteBuffer.get();
        fieldType = DBaseDataType.valueOfDataType(dt);

        // Field address.
        byteBuffer.get(fieldAddress);

        // Length and scale.
        fieldLength = byteBuffer.get();
        fieldDecimalCount = byteBuffer.get();

        byteBuffer.getShort(); // reserved

        byteBuffer.get(dbasePlusLanReserved2);

        // Work area id.
        workAreaID = byteBuffer.get();

        byteBuffer.get(dbasePlusLanReserved3);

        // Fields.
        setFields = byteBuffer.get();

        byte[] data = new byte[6];
        byteBuffer.get(data); // reserved
    }

    /**
     * Returns the decimal count of that field.
     * @return Decimal count.
     */
    public int getDecimalCount() {
        return Byte.toUnsignedInt(this.fieldDecimalCount);
    }
    
    /**
     * Returns the field length.
     * @return field length.
     */
    public int getLength() {
        return Byte.toUnsignedInt(this.fieldLength);
    }

    /**
     * Return the field name.
     * @return Field name.
     */
    public String getName() {
        int length = fieldName.length;
        while (length != 0 && fieldName[length - 1] <= ' ') {
            length--;
        }
        return new String(this.fieldName, 0, length);
    }

    /**
     * Return the field data type.
     * @return Data type.
     */
    public DBaseDataType getType() {
        return(fieldType);
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String text = format("toString", getName(), fieldType, fieldLength, fieldDecimalCount);
        return text;
    }
}
