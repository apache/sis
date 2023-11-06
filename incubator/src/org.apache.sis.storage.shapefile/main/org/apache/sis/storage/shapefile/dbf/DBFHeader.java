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

import org.apache.sis.io.stream.ChannelDataInput;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DBFHeader {

    private static final int FIELD_SIZE = 32;

    public int year;
    public int month;
    public int day;
    public int nbRecord;
    public int headerSize;
    public int recordSize;
    public DBFField[] fields;

    public void read(ChannelDataInput channel, Charset charset) throws IOException {
        channel.buffer.order(ByteOrder.LITTLE_ENDIAN);
        if (channel.readByte()!= 0x03) {
            throw new IOException("Unvalid database III magic");
        }
        year       = channel.readUnsignedByte();
        month      = channel.readUnsignedByte();
        day        = channel.readUnsignedByte();
        nbRecord   = channel.readInt();
        headerSize = channel.readUnsignedShort();
        recordSize = channel.readUnsignedShort();
        channel.skipBytes(20);
        fields     = new DBFField[(headerSize - FIELD_SIZE - 1) / FIELD_SIZE];

        for (int i = 0; i < fields.length; i++) {
            fields[i] = new DBFField();
            fields[i].read(channel, charset);
        }
        channel.skipBytes(1);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DBFHeader{");
        sb.append("year=").append(year);
        sb.append(",month=").append(month);
        sb.append(",day=").append(day);
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
