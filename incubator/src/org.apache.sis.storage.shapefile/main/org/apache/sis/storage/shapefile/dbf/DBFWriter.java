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
import org.apache.sis.io.stream.ChannelDataOutput;


/**
 * DBF writer.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DBFWriter implements AutoCloseable{

    private final ChannelDataOutput channel;
    private int writtenNbRecord = 0 ;
    private DBFHeader header;

    /**
     * Constructor.
     *
     * @param channel to write into
     */
    public DBFWriter(ChannelDataOutput channel) {
        this.channel = channel;
    }

    /**
     * Write DBase header.
     *
     * This should be the first call on the writer.
     * Header number of records will be updated in the close method.
     *
     * @param header to write
     * @throws IOException If an I/O error occurs
     */
    public void writeHeader(DBFHeader header) throws IOException {
        this.header = new DBFHeader(header);
        this.header.updateSizes(); //recompute sizes
        this.header.nbRecord = 0; //force to zero, will be replaced when closing writer.
        this.header.write(channel);
    }

    /**
     * Write a DBase record.
     *
     * @param fieldValues record fields to write
     * @throws IOException If an I/O error occurs
     */
    public void writeRecord(Object ... fieldValues) throws IOException {
        channel.writeByte(DBFReader.TAG_PRESENT);
        for (int i = 0; i < header.fields.length; i++) {
            header.fields[i].writeValue(channel, fieldValues[i]);
        }
        writtenNbRecord++;
    }

    /**
     * Write end of file tag, update written number of record and release resources.
     *
     * @throws IOException If an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        channel.writeByte(DBFReader.TAG_EOF);
        channel.flush();

        //update the nbRecord in the header
        channel.seek(4);
        channel.buffer.order(ByteOrder.LITTLE_ENDIAN);
        channel.writeInt(writtenNbRecord);
        channel.flush();

        channel.channel.close();
    }

}
