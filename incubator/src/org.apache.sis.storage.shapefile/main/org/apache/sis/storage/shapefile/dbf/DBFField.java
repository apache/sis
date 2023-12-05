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
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.ChannelDataOutput;


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

    public final String fieldName;
    public final char fieldType;
    public final int fieldAddress;
    public final int fieldLength;
    public final int fieldDecimals;
    public final Charset charset;
    public final Class valueClass;

    private final ReadMethod reader;
    private final WriteMethod writer;
    //used by decimal format only;
    private NumberFormat format;

    public DBFField(String fieldName, char fieldType, int fieldAddress, int fieldLength, int fieldDecimals, Charset charset) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.fieldAddress = fieldAddress;
        this.fieldLength = fieldLength;
        this.fieldDecimals = fieldDecimals;
        this.charset = charset;

        switch (Character.toLowerCase(fieldType)) {
            case TYPE_BINARY : valueClass = Long.class;      reader = this::readBinary; writer = this::writeBinary; break;
            case TYPE_CHAR :   valueClass = String.class;    reader = this::readChar;   writer = this::writeChar; break;
            case TYPE_DATE :   valueClass = LocalDate.class; reader = this::readDate;   writer = this::writeDate; break;
            case TYPE_NUMBER : {
                if (fieldDecimals != 0) {  valueClass = Double.class;  reader = this::readNumber;     writer = this::writeNumber;
                    format = NumberFormat.getNumberInstance(Locale.US);
                    format.setGroupingUsed(false);
                    format.setMaximumFractionDigits(fieldDecimals);
                    format.setMinimumFractionDigits(fieldDecimals);
                }
                else if (fieldLength > 9) {valueClass = Long.class;    reader = this::readNumberLong; writer = this::writeNumberLong;}
                else {                     valueClass = Integer.class; reader = this::readNumberInt;  writer = this::writeNumberInt;}
                break;
            }
            case TYPE_LOGIC :     valueClass = Boolean.class; reader = this::readLogic;         writer = this::writeLogic; break;
            case TYPE_MEMO :      valueClass = Object.class;  reader = this::readMemo;          writer = this::writeMemo; break;
            case TYPE_TIMESTAMP : valueClass = Object.class;  reader = this::readTimeStamp;     writer = this::writeTimeStamp; break;
            case TYPE_LONG :      valueClass = Object.class;  reader = this::readLong;          writer = this::writeLong; break;
            case TYPE_INC :       valueClass = Object.class;  reader = this::readAutoIncrement; writer = this::writeAutoIncrement; break;
            case TYPE_FLOAT :     valueClass = Object.class;  reader = this::readFloat;         writer = this::writeFloat; break;
            case TYPE_DOUBLE :    valueClass = Object.class;  reader = this::readDouble;        writer = this::writeDouble; break;
            case TYPE_OLE :       valueClass = Object.class;  reader = this::readOLE;           writer = this::writeOLE; break;
            default: throw new IllegalArgumentException("Unknown field type " + fieldType);
        }
    }

    public static DBFField read(ChannelDataInput channel, Charset charset) throws IOException {
        byte[] n = channel.readBytes(11);
        int nameSize = 0;
        for (int i = 0; i < n.length && n[i] != 0; i++,nameSize++);
        final String fieldName = new String(n, 0, nameSize);
        final char fieldType      = Character.valueOf(((char)channel.readUnsignedByte())).toString().charAt(0);
        final int fieldAddress   = channel.readInt();
        final int fieldLength    = channel.readUnsignedByte();
        final int fieldDecimals = channel.readUnsignedByte();
        channel.seek(channel.getStreamPosition() + 14);
        return new DBFField(fieldName, fieldType, fieldAddress, fieldLength, fieldDecimals, charset);
    }

    public void write(ChannelDataOutput channel) throws IOException {
        byte[] bytes = fieldName.getBytes(StandardCharsets.US_ASCII);
        if (bytes.length > 11) throw new IOException("Field name length must not be longer then 11 characters.");
        channel.write(bytes);
        channel.repeat(11 - bytes.length, (byte) 0);
        channel.writeByte(fieldType);
        channel.writeInt(fieldAddress);
        channel.writeByte(fieldLength);
        channel.writeByte(fieldDecimals);
        channel.repeat(14, (byte) 0);
    }

    public Object readValue(ChannelDataInput channel) throws IOException {
        return reader.readValue(channel);
    }

    public void writeValue(ChannelDataOutput channel, Object value) throws IOException {
        writer.writeValue(channel, value);
    }

    private Object readBinary(ChannelDataInput channel) throws IOException {
        throw new UnsupportedOperationException();
    }

    private Object readChar(ChannelDataInput channel) throws IOException {
        return new String(channel.readBytes(fieldLength), charset).trim();
    }

    private Object readDate(ChannelDataInput channel) throws IOException {
        final String str = new String(channel.readBytes(fieldLength)).trim();
        final int year = Integer.parseUnsignedInt(str,0,4,10);
        final int month = Integer.parseUnsignedInt(str,4,6,10);
        final int day = Integer.parseUnsignedInt(str,6,8,10);
        return LocalDate.of(year, month, day);
    }

    private Object readNumber(ChannelDataInput channel) throws IOException {
        final String str = new String(channel.readBytes(fieldLength)).trim();
        if (str.isEmpty()) return 0L;
        else return Double.parseDouble(str);
    }

    private Object readNumberInt(ChannelDataInput channel) throws IOException {
        final String str = new String(channel.readBytes(fieldLength)).trim();
        if (str.isEmpty()) return 0;
        else return Integer.parseInt(str);
    }

    private Object readNumberLong(ChannelDataInput channel) throws IOException {
        final String str = new String(channel.readBytes(fieldLength)).trim();
        if (str.isEmpty()) return 0L;
        else return Long.parseLong(str);
    }

    private Object readLogic(ChannelDataInput channel) throws IOException {
        final String str = new String(channel.readBytes(fieldLength)).trim().toLowerCase();
        final char c = str.charAt(0);
        switch (c) {
            case '1':
            case 't':
            case 'y':
                return Boolean.TRUE;
            case '0':
            case 'f':
            case 'n':
                return Boolean.FALSE;
            default:
                throw new IOException("Unexpected logic value : " + str);
        }
    }

    private Object readMemo(ChannelDataInput channel) throws IOException {
        throw new UnsupportedOperationException();
    }

    private Object readTimeStamp(ChannelDataInput channel) throws IOException {
        throw new UnsupportedOperationException();
    }

    private Object readLong(ChannelDataInput channel) throws IOException {
        throw new UnsupportedOperationException();
    }

    private Object readAutoIncrement(ChannelDataInput channel) throws IOException {
        throw new UnsupportedOperationException();
    }

    private Object readFloat(ChannelDataInput channel) throws IOException {
        return channel.readFloat();
    }

    private Object readDouble(ChannelDataInput channel) throws IOException {
        return channel.readDouble();
    }

    private Object readOLE(ChannelDataInput channel) throws IOException {
        throw new UnsupportedOperationException();
    }

    private void writeBinary(ChannelDataOutput channel, Object value) throws IOException {
        throw new UnsupportedOperationException();
    }

    private void writeChar(ChannelDataOutput channel, Object value) throws IOException {
        final String txt = (String) value;
        final byte[] bytes = txt.getBytes(charset);
        if (bytes.length >= fieldLength) {
            channel.write(bytes, 0, fieldLength);
        } else {
            channel.write(bytes);
            channel.repeat(fieldLength - bytes.length, (byte)' ');
        }
    }

    private void writeDate(ChannelDataOutput channel, Object value) throws IOException {
        final LocalDate date = (LocalDate) value;
        final StringBuilder sb = new StringBuilder();
        String year = Integer.toString(date.getYear());
        String month = Integer.toString(date.getMonthValue());
        String day = Integer.toString(date.getDayOfMonth());
        switch (year.length()) {
            case 1: sb.append("000"); break;
            case 2: sb.append("00"); break;
            case 3: sb.append("0"); break;
        }
        sb.append(year);
        if(month.length() < 2) sb.append("0");
        sb.append(month);
        if(day.length() < 2) sb.append("0");
        sb.append(day);
        ensureLength(sb.toString());
        channel.repeat(fieldLength - sb.length(), (byte)' ');
        channel.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
    }

    private void writeNumber(ChannelDataOutput channel, Object value) throws IOException {
        final Number v = ((Number) value);
        final String str = format.format(v.doubleValue());
        final int length = str.length();
        ensureLength(str);
        channel.repeat(fieldLength - length, (byte)' ');
        channel.write(str.getBytes(StandardCharsets.US_ASCII));
    }

    private void writeNumberInt(ChannelDataOutput channel, Object value) throws IOException {
        final String str = ((Integer) value).toString();
        ensureLength(str);
        channel.repeat(fieldLength - str.length(), (byte)' ');
        channel.write(str.getBytes(StandardCharsets.US_ASCII));
    }

    private void writeNumberLong(ChannelDataOutput channel, Object value) throws IOException {
        final String str = ((Long) value).toString();
        ensureLength(str);
        channel.repeat(fieldLength - str.length(), (byte)' ');
        channel.write(str.getBytes(StandardCharsets.US_ASCII));
    }

    private void writeLogic(ChannelDataOutput channel, Object value) throws IOException {
        final String str = Boolean.TRUE.equals(value) ? "T" : "F";
        ensureLength(str);
        channel.repeat(fieldLength - str.length(), (byte)' ');
        channel.write(str.getBytes(StandardCharsets.US_ASCII));
    }

    private void writeMemo(ChannelDataOutput channel, Object value) throws IOException {
        throw new UnsupportedOperationException();
    }

    private void writeTimeStamp(ChannelDataOutput channel, Object value) throws IOException {
        throw new UnsupportedOperationException();
    }

    private void writeLong(ChannelDataOutput channel, Object value) throws IOException {
        throw new UnsupportedOperationException();
    }

    private void writeAutoIncrement(ChannelDataOutput channel, Object value) throws IOException {
        throw new UnsupportedOperationException();
    }

    private void writeFloat(ChannelDataOutput channel, Object value) throws IOException {
        channel.writeFloat(((Number)value).floatValue());
    }

    private void writeDouble(ChannelDataOutput channel, Object value) throws IOException {
        channel.writeDouble(((Number)value).doubleValue());
    }

    private void writeOLE(ChannelDataOutput channel, Object value) throws IOException {
        throw new UnsupportedOperationException();
    }

    private void ensureLength(String str) throws IOException{
        final int remain = fieldLength - str.length();
        if (remain < 0) throw new IOException("Failed to write field value" + str + ", value is larger then field " + this);
    }

    @Override
    public String toString() {
        return "DBFField{" +
                "fieldName='" + fieldName + '\'' +
                ", fieldType=" + fieldType +
                ", fieldAddress=" + fieldAddress +
                ", fieldLength=" + fieldLength +
                ", fieldLDecimals=" + fieldDecimals +
                '}';
    }

    private interface ReadMethod {
        Object readValue(ChannelDataInput channel) throws IOException;
    }

    private interface WriteMethod {
        void writeValue(ChannelDataOutput channel, Object value) throws IOException;
    }

}
