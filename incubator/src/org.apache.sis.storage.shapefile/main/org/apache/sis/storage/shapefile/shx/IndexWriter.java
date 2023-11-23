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

import org.apache.sis.storage.shapefile.shp.ShapeHeader;
import org.apache.sis.io.stream.ChannelDataOutput;
import java.io.IOException;

/**
 * Shape file writer.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class IndexWriter implements AutoCloseable{

    private final ChannelDataOutput channel;

    private ShapeHeader header;

    public IndexWriter(ChannelDataOutput channel) throws IOException {
        this.channel = channel;
    }

    public ShapeHeader getHeader() {
        return header;
    }
    /**
     * Header will be copied and modified.
     * Use getHeader to obtain the new header.
     */
    public void write(ShapeHeader header) throws IOException {
        this.header = new ShapeHeader(header);
        this.header.fileLength = 0;
        header.write(channel);
    }

    public void write(int offset, int length) throws IOException {
        channel.writeInt(offset);
        channel.writeInt(length);
    }

    public void flush() throws IOException {
        channel.flush();
    }

    @Override
    public void close() throws IOException {
        flush();

        //update header and rewrite it
        //update the file length
        header.fileLength = (int) channel.getStreamPosition();
        channel.seek(0);
        header.write(channel);
        channel.flush();

        channel.channel.close();
    }

}
