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

import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.ChannelDataOutput;
import org.locationtech.jts.geom.Geometry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author Johann Sorel (Geomatys)
 */
public final class ShapeRecord {

    /**
     * Record number.
     */
    public int recordNumber;
    /**
     * Encoded geometry.
     */
    public byte[] content;

    public Geometry geometry;

    public GeneralEnvelope bbox;

    /**
     * Read this shape record.
     * @param channel input channel, not null
     * @throws IOException if an error occurred while reading.
     */
    public void read(final ChannelDataInput channel) throws IOException {
        channel.buffer.order(ByteOrder.BIG_ENDIAN);
        recordNumber = channel.readInt();
        content = channel.readBytes(channel.readInt() * 2); // x2 because size is in 16bit words
    }

    public void parseGeometry(ShapeGeometryEncoder io) throws IOException {
        final ChannelDataInput di = new ChannelDataInput("", ByteBuffer.wrap(content));
        di.buffer.order(ByteOrder.LITTLE_ENDIAN);
        int shapeType = di.readInt();
        io.decode(di,this);
    }

    /**
     * Write this shape record.
     * @param channel output channel to write into, not null
     * @param io geometry encoder
     * @throws IOException
     */
    public void write(ChannelDataOutput channel, ShapeGeometryEncoder io) throws IOException {
        channel.buffer.order(ByteOrder.BIG_ENDIAN);
        channel.writeInt(recordNumber);
        channel.writeInt((io.getEncodedLength(geometry) + 4) / 2); // +4 for shape type and /2 because size is in 16bit words
        channel.buffer.order(ByteOrder.LITTLE_ENDIAN);
        channel.writeInt(io.getShapeType());
        io.encode(channel, this);
    }
}
