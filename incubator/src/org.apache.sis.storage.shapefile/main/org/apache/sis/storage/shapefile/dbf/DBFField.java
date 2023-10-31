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
package org.apache.sis.storage.shapefile.dbf;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.util.ArraysExt;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DBFField {

    /**
     * Binary, String : b or B
     */
    public static final int TYPE_BINARY = 'b';
    /**
     * Characters : c or C
     */
    public static final int TYPE_CHAR = 'c';
    /**
     * Date : d or D
     */
    public static final int TYPE_DATE = 'd';
    /**
     * Numeric : n or N
     */
    public static final int TYPE_NUMBER = 'n';
    /**
     * Logical : l or L
     */
    public static final int TYPE_LOGIC = 'l';
    /**
     * Memo, String : m or M
     */
    public static final int TYPE_MEMO = 'm';
    /**
     * TimeStamp : 8 bytes, two longs, first for date, second for time.
     * The date is the number of days since  01/01/4713 BC.
     * Time is hours * 3600000L + minutes * 60000L + Seconds * 1000L
     */
    public static final int TYPE_TIMESTAMP = '@';
    /**
     * Long : i or I on 4 bytes, first bit is the sign, 0 = negative
     */
    public static final int TYPE_LONG = 'i';
    /**
     * Autoincrement : same as Long
     */
    public static final int TYPE_INC = '+';
    /**
     * Floats : f or F
     */
    public static final int TYPE_FLOAT = 'f';
    /**
     * Double : o or O, real double on 8bytes, not string encoded
     */
    public static final int TYPE_DOUBLE = 'o';
    /**
     * OLE : g or G
     */
    public static final int TYPE_OLE = 'g';

    public String fieldName;
    public int fieldType;
    public int fieldAddress;
    public int fieldLength;
    public int fieldLDecimals;

    private DBFFieldEncoder encoder;

    public void read(ChannelDataInput channel, Charset charset) throws IOException {
        byte[] n = channel.readBytes(11);
        int nameSize = 0;
        for (int i = 0; i < n.length && n[i] != 0; i++,nameSize++);

        fieldName      = new String(n, 0, nameSize);
        fieldType      = Character.valueOf(((char)channel.readUnsignedByte())).toString().toLowerCase().charAt(0);
        fieldAddress   = channel.readInt();
        fieldLength    = channel.readUnsignedByte();
        fieldLDecimals = channel.readUnsignedByte();
        channel.skipBytes(14);

        encoder = DBFFieldEncoder.getEncoder(fieldType, fieldLength, fieldLDecimals, charset);
    }

    public DBFFieldEncoder getEncoder() {
        return encoder;
    }

    @Override
    public String toString() {
        return "DBFField{" +
                "fieldName='" + fieldName + '\'' +
                ", fieldType=" + fieldType +
                ", fieldAddress=" + fieldAddress +
                ", fieldLength=" + fieldLength +
                ", fieldLDecimals=" + fieldLDecimals +
                '}';
    }
}
