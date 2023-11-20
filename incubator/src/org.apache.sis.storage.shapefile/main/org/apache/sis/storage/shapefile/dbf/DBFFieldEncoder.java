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
import static org.apache.sis.storage.shapefile.dbf.DBFField.*;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class DBFFieldEncoder {

    public static DBFFieldEncoder getEncoder(char fieldType, int fieldLength, int fieldDecimals, Charset charset) {
        switch (Character.toLowerCase(fieldType)) {
            case TYPE_BINARY : return new Binary(fieldLength, fieldDecimals);
            case TYPE_CHAR : return new Char(fieldLength, fieldDecimals, charset);
            case TYPE_DATE : return new Date(fieldLength, fieldDecimals);
            case TYPE_NUMBER : {
                if (fieldDecimals != 0) return new Decimal(fieldLength, fieldDecimals);
                return fieldLength > 9 ? new LongInt(fieldLength) : new ShortInt(fieldLength);
            }
            case TYPE_LOGIC : return new Logic(fieldLength);
            case TYPE_MEMO : throw new UnsupportedOperationException("todo");
            case TYPE_TIMESTAMP : throw new UnsupportedOperationException("todo");
            case TYPE_LONG : throw new UnsupportedOperationException("todo");
            case TYPE_INC : throw new UnsupportedOperationException("todo");
            case TYPE_FLOAT : throw new UnsupportedOperationException("todo");
            case TYPE_DOUBLE : throw new UnsupportedOperationException("todo");
            case TYPE_OLE : throw new UnsupportedOperationException("todo");
            default: throw new IllegalArgumentException("Unknown field type "+fieldType);
        }

    }

    protected final Class valueClass;
    protected final int fieldLength;
    protected final int fieldLDecimals;

    public DBFFieldEncoder(Class valueClass, int fieldLength, int fieldLDecimals) {
        this.valueClass = valueClass;
        this.fieldLength = fieldLength;
        this.fieldLDecimals = fieldLDecimals;
    }


    public Class getValueClass() {
        return valueClass;
    }

    public abstract Object read(ChannelDataInput channel) throws IOException;

    public abstract void write(ChannelDataOutput channel, Object value) throws IOException;


    private static final class Binary extends DBFFieldEncoder {

        public Binary(int fieldLength, int fieldDecimals) {
            super(Long.class, fieldLength, fieldDecimals);
        }

        @Override
        public Object read(ChannelDataInput channel) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void write(ChannelDataOutput channel, Object value) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private static final class Char extends DBFFieldEncoder {

        private final Charset charset;

        public Char(int fieldLength, int fieldDecimals, Charset charset) {
            super(String.class, fieldLength, fieldDecimals);
            this.charset = charset;
        }

        @Override
        public Object read(ChannelDataInput channel) throws IOException {
            return new String(channel.readBytes(fieldLength), charset).trim();
        }

        @Override
        public void write(ChannelDataOutput channel, Object value) throws IOException {
            final String txt = (String) value;
            final byte[] bytes = txt.getBytes(charset);
            if (bytes.length >= fieldLength) {
                channel.write(bytes, 0, fieldLength);
            } else {
                channel.write(bytes);
                channel.repeat(fieldLength - bytes.length, (byte)' ');
            }
        }
    }

    private static final class Date extends DBFFieldEncoder {

        public Date(int fieldLength, int fieldDecimals) {
            super(LocalDate.class, fieldLength, fieldDecimals);
        }

        @Override
        public Object read(ChannelDataInput channel) throws IOException {
            final String str = new String(channel.readBytes(fieldLength)).trim();
            final int year = Integer.parseUnsignedInt(str,0,4,10);
            final int month = Integer.parseUnsignedInt(str,4,6,10);
            final int day = Integer.parseUnsignedInt(str,6,8,10);
            return LocalDate.of(year, month, day);
        }

        @Override
        public void write(ChannelDataOutput channel, Object value) throws IOException {
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
            channel.repeat(fieldLength - sb.length(), (byte)' ');
            channel.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
        }
    }

    private static final class ShortInt extends DBFFieldEncoder {

        public ShortInt(int fieldLength) {
            super(Integer.class, fieldLength, 0);
        }

        @Override
        public Object read(ChannelDataInput channel) throws IOException {
            final String str = new String(channel.readBytes(fieldLength)).trim();
            if (str.isEmpty()) return 0;
            else return Integer.parseInt(str);
        }

        @Override
        public void write(ChannelDataOutput channel, Object value) throws IOException {
            final String v = ((Integer) value).toString();
            final int length = v.length();
            if (length > fieldLength) {
                throw new IOException(v + " is longer then field length " + fieldLength);
            }
            channel.repeat(fieldLength - length, (byte)' ');
            channel.write(v.getBytes(StandardCharsets.US_ASCII));
        }
    }

    private static final class LongInt extends DBFFieldEncoder {

        public LongInt(int fieldLength) {
            super(Long.class, fieldLength, 0);
        }

        @Override
        public Object read(ChannelDataInput channel) throws IOException {
            final String str = new String(channel.readBytes(fieldLength)).trim();
            if (str.isEmpty()) return 0L;
            else return Long.parseLong(str);
        }

        @Override
        public void write(ChannelDataOutput channel, Object value) throws IOException {
            final String v = ((Long) value).toString();
            final int length = v.length();
            if (length > fieldLength) {
                throw new IOException(v + " is longer then field length " + fieldLength);
            }
            channel.repeat(fieldLength - length, (byte)' ');
            channel.write(v.getBytes(StandardCharsets.US_ASCII));
        }
    }

    private static final class Decimal extends DBFFieldEncoder {
        private final NumberFormat format = NumberFormat.getNumberInstance(Locale.US);

        public Decimal(int fieldLength, int fieldDecimals) {
            super(Double.class, fieldLength, fieldDecimals);
            format.setMaximumFractionDigits(fieldDecimals);
            format.setMinimumFractionDigits(fieldDecimals);
        }

        @Override
        public Object read(ChannelDataInput channel) throws IOException {
            final String str = new String(channel.readBytes(fieldLength)).trim();
            if (str.isEmpty()) return 0L;
            else return Double.parseDouble(str);
        }

        @Override
        public void write(ChannelDataOutput channel, Object value) throws IOException {
            final Number v = ((Number) value);
            final String str =format.format(v.doubleValue());
            final int length = str.length();
            channel.repeat(fieldLength - length, (byte)' ');
            channel.write(str.getBytes(StandardCharsets.US_ASCII));
        }
    }

    private static final class Logic extends DBFFieldEncoder {

        public Logic(int fieldLength) {
            super(Boolean.class, fieldLength, 0);
        }

        @Override
        public Object read(ChannelDataInput channel) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void write(ChannelDataOutput channel, Object value) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

}
