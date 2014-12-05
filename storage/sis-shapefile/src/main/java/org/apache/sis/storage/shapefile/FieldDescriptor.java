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
package org.apache.sis.storage.shapefile;

import org.apache.sis.util.logging.AbstractAutoChecker;


/**
 * Field descriptor.
 *
 * @author  Travis L. Pinney
 * @since   0.5
 * @version 0.5
 * @module
 */
public class FieldDescriptor extends AbstractAutoChecker {
    /** Field name. */
    public byte[] FieldName = new byte[11];

    /** Field type. */
    public DataType FieldType;

    /** Field address (Field data address (address is set in memory; not useful on disk). */
    public byte[] FieldAddress = new byte[4];

    /** Field length. */
    public byte FieldLength;

    /** Decimal count. */
    public byte FieldDecimalCount;

    /** Reserved 2. */
    public byte[] DbasePlusLanReserved2 = new byte[2];

    /** Work area id. */
    public byte WorkAreaID;

    /** Reserved 3. */
    public byte[] DbasePlusLanReserved3 = new byte[2];

    public byte SetFields;
    
    /**
     * Returns the decimal count of that field.
     * @return Decimal count.
     */
    public int getDecimalCount() {
        return Byte.toUnsignedInt(this.FieldDecimalCount);
    }
    
    /**
     * Returns the field length.
     * @return field length.
     */
    public int getLength() {
        return Byte.toUnsignedInt(this.FieldLength);
    }

    /**
     * Return the field name.
     * @return Field name.
     */
    public String getName() {
        int length = FieldName.length;
        while (length != 0 && FieldName[length - 1] <= ' ') {
            length--;
        }
        return new String(this.FieldName, 0, length);
    }

    /**
     * Return the field data type.
     * @return Data type.
     */
    public DataType getType() {
        return(FieldType);
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String text = format("toString", getName(), FieldType, FieldLength, FieldDecimalCount);
        return text;
        
        // s.append("DbasePlusLanReserved2: ").append(DbasePlusLanReserved2 + "\n");
        // s.append("WorkAreaID: ").append(WorkAreaID).append(lineSeparator);
        // s.append("DbasePlusLanReserved3: ").append(DbasePlusLanReserved3).append(lineSeparator);
        // s.append("SetFields: ").append(SetFields).append(lineSeparator);
    }
}
