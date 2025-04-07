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
import java.time.ZoneId;
import org.apache.sis.io.stream.ChannelDataInput;


/**
 * Seekable DBase file reader.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DBFReader implements AutoCloseable {

    /**
     * Unique instance used to mark a record which has been deleted.
     */
    public static final Object[] DELETED_RECORD = new Object[0];

    static final int TAG_PRESENT = 0x20;
    static final int TAG_DELETED = 0x2a;
    static final int TAG_EOF = 0x1A;

    private final ChannelDataInput channel;
    private final DBFHeader header;
    private final int[] fieldsToRead;
    private int nbRead = 0;

    /**
     * Constructor.
     *
     * @param channel to read from
     * @param charset text encoding
     * @param fieldsToRead fields index in the header to decode, other fields will be skipped. must be in increment order.
     * @throws IOException if a decoding error occurs on the header
     */
    public DBFReader(ChannelDataInput channel, Charset charset, ZoneId timezone, int[] fieldsToRead) throws IOException {
        this.channel = channel;
        this.header = new DBFHeader();
        this.header.read(channel, charset, timezone);
        this.fieldsToRead = fieldsToRead;
    }

    /**
     * Get decoded header.
     *
     * @return Dbase file header
     */
    public DBFHeader getHeader() {
        return header;
    }

    /**
     * Move channel to given position.
     *
     * @param position new position
     * @throws IOException if the stream cannot be moved to the given position.
     */
    public void moveToOffset(long position) throws IOException {
        channel.seek(position);
    }

    /**
     * Get next record.
     *
     * @return record or DBFReader.DELETED_RECORD if this record has been deleted.
     * @throws IOException if a decoding error occurs
     */
    public Object[] next() throws IOException {
        if (nbRead >= header.nbRecord) {
            //reached records end
            //we do not trust the EOF if we already have the expected count
            //some incorrect files do not have it
            return null;
        }
        nbRead++;

        final int marker = channel.readUnsignedByte();
        if (marker == TAG_DELETED) {
            channel.seek(channel.getStreamPosition() + header.recordSize);
            return DELETED_RECORD;
        } else if (marker == TAG_EOF) {
            return null;
        } else if (marker != TAG_PRESENT) {
            throw new IOException("Unexpected record marker " + marker);
        }

        Object[] record;
        if (fieldsToRead == null) {
            //read all fields
            record = new Object[header.fields.length];
            for (int i = 0; i < header.fields.length; i++) {
                record[i] = header.fields[i].readValue(channel);
            }
        } else {
            //read only selected fields
            record = new Object[fieldsToRead.length];
            for (int i = 0,k = 0; i < header.fields.length; i++) {
                if (k < fieldsToRead.length && fieldsToRead[k] == i) {
                    record[k++] = header.fields[i].readValue(channel);
                } else {
                    //skip this field
                    channel.seek(channel.getStreamPosition() + header.fields[i].fieldLength);
                }
            }
        }

        return record;
    }

    /**
     * Release reader resources.
     *
     * @throws IOException If an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        channel.channel.close();
    }

}
