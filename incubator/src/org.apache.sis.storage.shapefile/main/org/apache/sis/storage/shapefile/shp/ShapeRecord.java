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

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.ByteOrder;
import org.locationtech.jts.geom.Geometry;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.ChannelDataOutput;


/**
 * A record in a shape file.
 * Contains a unique record number and it's associated geometry.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ShapeRecord {

    /**
     * Record number.
     * Starts at 1 in the file.
     */
    public int recordNumber;
    /**
     * Record geometry
     */
    public Geometry geometry;
    /**
     * Geometry bounding box
     */
    public GeneralEnvelope bbox;

    /**
     * Default constructor
     */
    public ShapeRecord() {
    }

    /**
     * Constructor with initialization.
     *
     * @param recordNumber initial record number
     * @param geometry initial geometry
     */
    public ShapeRecord(int recordNumber, Geometry geometry) {
        this.recordNumber = recordNumber;
        this.geometry = geometry;
    }

    /**
     * Read this shape record.
     *
     * A record declaring the {@link ShapeType#NULL} type has no geometry,
     * such record may be found in a file of any other shape type.
     *
     * @param channel input channel, not null
     * @param io geometry decoder, not null
     * @param filter optional filter envelope to stop geometry decoding as soon as possible
     * @return true if geometry pass the filter or if there is no filter
     * @throws IOException if an error occurred while reading.
     */
    public boolean read(final ChannelDataInput channel, ShapeGeometryEncoder io, Rectangle2D.Double filter) throws IOException {
        if (io == null) throw new IllegalArgumentException("encoder must not be null");

        channel.buffer.order(ByteOrder.BIG_ENDIAN);
        recordNumber = channel.readInt();
        final int byteSize = channel.readInt() * 2; // x2 because size is in 16bit words
        final long position = channel.getStreamPosition();
        channel.buffer.order(ByteOrder.LITTLE_ENDIAN);
        final int shapeType = channel.readInt();
        final boolean match;
        if (shapeType == ShapeType.NULL.getCode()) {
            //this record has no geometry, it can never match a filter area
            geometry = null;
            bbox = null;
            match = filter == null;
        } else {
            match = io.decode(channel,this, filter);
        }
        //always move to record end, size is sometime larger then the geometry bytes
        channel.seek(position + byteSize);
        return match;
    }

    /**
     * Write this shape record.
     *
     * If the geometry is null the record is written as a {@link ShapeType#NULL} shape.
     *
     * @param channel output channel to write into, not null
     * @param io geometry encoder
     * @throws IOException if an error occurred while writing.
     */
    public void write(ChannelDataOutput channel, ShapeGeometryEncoder io) throws IOException {
        channel.buffer.order(ByteOrder.BIG_ENDIAN);
        channel.writeInt(recordNumber);
        if (geometry == null) {
            channel.writeInt(2); // the record contains only the 4 bytes of the shape type, size is in 16bit words
            channel.buffer.order(ByteOrder.LITTLE_ENDIAN);
            channel.writeInt(ShapeType.NULL.getCode());
            return;
        }
        channel.writeInt((io.getEncodedLength(geometry) + 4) / 2); // +4 for shape type and /2 because size is in 16bit words
        channel.buffer.order(ByteOrder.LITTLE_ENDIAN);
        channel.writeInt(io.getShapeType().getCode());
        io.encode(channel, this);
    }
}
