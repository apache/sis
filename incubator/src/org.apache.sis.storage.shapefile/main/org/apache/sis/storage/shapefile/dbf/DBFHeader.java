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
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.ZoneId;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.ChannelDataOutput;


/**
 * Mutable DBase III file header.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DBFHeader {

    private static final int FIELD_SIZE = 32;
    private static final int FIELD_DESCRIPTOR_TERMINATOR = 0x0D;

    /**
     * Date of last update.
     */
    public LocalDate lastUpdate;
    /**
     * Number of records in the file.
     */
    public int nbRecord;
    /**
     * Total header size.
     */
    public int headerSize;
    /**
     * Size of a single record.
     */
    public int recordSize;
    /**
     * Array of field descriptors.
     */
    public DBFField[] fields;

    /**
     * Default constructor.
     */
    public DBFHeader() {
    }

    /**
     * Duplicate constructor.
     *
     * @param toCopy to copy from
     */
    public DBFHeader(DBFHeader toCopy) {
        this.lastUpdate = toCopy.lastUpdate;
        this.nbRecord = toCopy.nbRecord;
        this.headerSize = toCopy.headerSize;
        this.recordSize = toCopy.recordSize;
        this.fields = new DBFField[toCopy.fields.length];
        System.arraycopy(toCopy.fields, 0, this.fields, 0, this.fields.length);
    }

    /**
     * Compute and update header size and record size.
     */
    public void updateSizes() {
        headerSize = 32 + FIELD_SIZE * fields.length + 1;
        recordSize = 1; //record state tag
        for (DBFField field : fields) {
            //ensure we do not overflow because of very large incorrect field length.
            recordSize = Math.addExact(recordSize, field.fieldLength);
        }
    }

    /**
     * Read header.
     *
     * @param channel to read from
     * @param charset field text encoding
     * @throws IOException if an error occured while parsing header
     */
    public void read(ChannelDataInput channel, Charset charset, ZoneId timezone) throws IOException {
        channel.buffer.order(ByteOrder.LITTLE_ENDIAN);
        if (channel.readByte()!= 0x03) {
            throw new IOException("Unvalid database III magic");
        }
        int year       = channel.readUnsignedByte();
        int month      = channel.readUnsignedByte();
        int day        = channel.readUnsignedByte();
        lastUpdate = LocalDate.of(year+1900, month, day);
        nbRecord   = channel.readInt();
        headerSize = channel.readUnsignedShort();
        recordSize = channel.readUnsignedShort();
        channel.seek(channel.getStreamPosition() + 20);
        fields     = new DBFField[(headerSize - FIELD_SIZE - 1) / FIELD_SIZE];

        for (int i = 0; i < fields.length; i++) {
            fields[i] = DBFField.read(channel, charset, timezone);
        }
        if (channel.readByte()!= FIELD_DESCRIPTOR_TERMINATOR) {
            throw new IOException("Unvalid database III field descriptor terminator");
        }
    }

    /**
     * Write header.
     *
     * @param channel to write into
     * @throws IOException if an error occured while writing header
     */
    public void write(ChannelDataOutput channel) throws IOException {
        channel.buffer.order(ByteOrder.LITTLE_ENDIAN);
        channel.writeByte(0x03);
        channel.writeByte(lastUpdate.getYear()-1900);
        channel.writeByte(lastUpdate.getMonthValue());
        channel.writeByte(lastUpdate.getDayOfMonth());
        channel.writeInt(nbRecord);
        channel.writeShort(headerSize);
        channel.writeShort(recordSize);
        channel.repeat(20, (byte)0);
        for (int i = 0; i < fields.length; i++) {
            fields[i].write(channel);
        }
        channel.writeByte(FIELD_DESCRIPTOR_TERMINATOR);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DBFHeader{");
        sb.append("lastUpdate=").append(lastUpdate);
        sb.append(",nbRecord=").append(nbRecord);
        sb.append(",headerSize=").append(headerSize);
        sb.append(",recordSize=").append(recordSize);
        sb.append("}\n");
        for (DBFField field : fields) {
            sb.append("- ").append(field.toString());
            sb.append("\n");
        }
        return sb.toString();
    }
}
