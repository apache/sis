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
package org.apache.sis.storage.shapefile.shp;

import org.apache.sis.io.stream.ChannelDataOutput;

import java.io.IOException;
import java.nio.ByteOrder;

/**
 * Shape file writer.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ShapeWriter implements AutoCloseable{

    private final ChannelDataOutput channel;

    private ShapeGeometryEncoder io;

    public ShapeWriter(ChannelDataOutput channel) throws IOException {
        this.channel = channel;
    }

    public void write(ShapeHeader header) throws IOException {
        header.write(channel);
        io = ShapeGeometryEncoder.getEncoder(header.shapeType);
    }

    public void write(ShapeRecord record) throws IOException {
        record.write(channel, io);
    }

    public void flush() throws IOException {
        channel.flush();
    }

    @Override
    public void close() throws IOException {
        channel.flush();

        //update the file length in the header
        final long fileLength = channel.getStreamPosition();
        channel.seek(24);
        channel.buffer.order(ByteOrder.BIG_ENDIAN);
        channel.writeInt((int) fileLength);

        channel.channel.close();
    }

}
