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
import org.apache.sis.io.stream.ChannelDataInput;


/**
 * Seekable dbf file reader.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DBFReader implements AutoCloseable {

    static final int TAG_PRESENT = 0x20;
    static final int TAG_DELETED = 0x2a;
    static final int TAG_EOF = 0x1A;

    private final ChannelDataInput channel;
    private final DBFHeader header;
    private final int[] fieldsToRead;
    private int nbRead = 0;

    /**
     * @param channel to read from
     * @param charset text encoding
     * @param fieldsToRead fields index in the header to decode, other fields will be skipped. must be in increment order.
     */
    public DBFReader(ChannelDataInput channel, Charset charset, int[] fieldsToRead) throws IOException {
        this.channel = channel;
        this.header = new DBFHeader();
        this.header.read(channel, charset);
        this.fieldsToRead = fieldsToRead;
    }

    public DBFHeader getHeader() {
        return header;
    }

    public void moveToOffset(long position) throws IOException {
        channel.seek(position);
    }

    /**
     *
     * @return record or DBFRecord.DELETED if this record has been deleted.
     * @throws IOException if a decoding error occurs
     */
    public DBFRecord next() throws IOException {
        if (nbRead >= header.nbRecord) {
            //reached records end
            //we do not trust the EOF if we already have the expected count
            //some writes do not have it
            return null;
        }
        nbRead++;

        final int marker = channel.readUnsignedByte();
        if (marker == TAG_DELETED) {
            channel.seek(channel.getStreamPosition() + header.recordSize);
            return DBFRecord.DELETED;
        } else if (marker == TAG_EOF) {
            return null;
        } else if (marker != TAG_PRESENT) {
            throw new IOException("Unexpected record marker " + marker);
        }

        final DBFRecord record = new DBFRecord();
        record.fields = new Object[header.fields.length];
        if (fieldsToRead == null) {
            //read all fields
            record.fields = new Object[header.fields.length];
            for (int i = 0; i < header.fields.length; i++) {
                record.fields[i] = header.fields[i].readValue(channel);
            }
        } else {
            //read only selected fields
            record.fields = new Object[fieldsToRead.length];
            for (int i = 0,k = 0; i < header.fields.length; i++) {
                if (k < fieldsToRead.length && fieldsToRead[k] == i) {
                    record.fields[k++] = header.fields[i].readValue(channel);
                } else {
                    //skip this field
                    channel.seek(channel.getStreamPosition() + header.fields[i].fieldLength);
                }
            }
        }

        return record;
    }



    @Override
    public void close() throws IOException {
        channel.channel.close();
    }

}
