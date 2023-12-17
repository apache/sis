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
package org.apache.sis.storage.shapefile.shx;

import java.io.EOFException;
import java.io.IOException;
import org.apache.sis.storage.shapefile.shp.*;
import org.apache.sis.io.stream.ChannelDataInput;


/**
 * Seekable shx index file reader.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class IndexReader implements AutoCloseable{

    private final ChannelDataInput channel;
    private final ShapeHeader header;

    /**
     * Constructor.
     *
     * @param channel to read from
     * @throws IOException if a decoding error occurs on the header
     */
    public IndexReader(ChannelDataInput channel) throws IOException {
        this.channel = channel;
        header = new ShapeHeader();
        header.read(channel);
    }

    /**
     * Get shapefile header.
     *
     * @return shapefile header
     */
    public ShapeHeader getHeader() {
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
     * @return next offset and length of the record in the shp file, null if no more records
     * @throws IOException if a decoding error occurs
     */
    public int[] next() throws IOException {
        try {
            return channel.readInts(2);
        } catch (EOFException ex) {
            //no more records
            return null;
        }
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
