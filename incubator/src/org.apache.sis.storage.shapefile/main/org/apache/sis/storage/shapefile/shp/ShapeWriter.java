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

import java.io.IOException;
import org.apache.sis.io.stream.ChannelDataOutput;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.locationtech.jts.geom.Geometry;


/**
 * Shape file writer.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ShapeWriter implements AutoCloseable{

    private final ChannelDataOutput channel;

    private ShapeHeader header;
    private GeneralEnvelope bbox;
    private ShapeGeometryEncoder io;

    /**
     * Construct writer above given channel.
     *
     * @param channel to write into
     */
    public ShapeWriter(ChannelDataOutput channel) {
        this.channel = channel;
    }

    /**
     * Get header, not null after writeHeader has been call.
     * @return shapefile header
     */
    public ShapeHeader getHeader() {
        return header;
    }

    /**
     * Get current position in the stream.
     *
     * @return current position in the stream
     */
    public long getSteamPosition() {
        return channel.getBitOffset();
    }

    /**
     * Header will be copied and modified.
     * Use getHeader to obtain the new header.
     * @param header to write, not null
     * @throws IOException If an I/O error occurs
     */
    public void writeHeader(ShapeHeader header) throws IOException {
        this.header = new ShapeHeader(header);
        this.header.fileLength = 0;
        this.header.bbox = new ImmutableEnvelope(new GeneralEnvelope(4));
        header.write(channel);
        io = ShapeGeometryEncoder.getEncoder(header.shapeType);
    }

    /**
     * Write a new record.
     *
     * @param recordNumber record number
     * @param geometry record geometry
     * @throws IOException If an I/O error occurs
     */
    public void writeRecord(int recordNumber, Geometry geometry) throws IOException {
        writeRecord(new ShapeRecord(recordNumber, geometry));
    }

    /**
     * Write a new record.
     *
     * @param record new record
     * @throws IOException If an I/O error occurs
     */
    public void writeRecord(ShapeRecord record) throws IOException {
        record.write(channel, io);
        final GeneralEnvelope geomBox = io.getBoundingBox(record.geometry);
        if (bbox == null) {
            bbox = new GeneralEnvelope(geomBox.getDimension());
            bbox.setEnvelope(geomBox);
        } else {
            bbox.add(geomBox);
        }
    }

    /**
     * Release writer resources.
     *
     * @throws IOException If an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        channel.flush();

        //update header and rewrite it
        //update the file length
        header.fileLength = (int) channel.getStreamPosition();

        //update bbox, size must be 4
        if (bbox != null) {
            if (bbox.getDimension() == 4) {
                header.bbox = new ImmutableEnvelope(bbox);
            } else {
                final GeneralEnvelope e = new GeneralEnvelope(4);
                for (int i = 0, n = bbox.getDimension(); i < n; i++) {
                    e.setRange(i, bbox.getMinimum(i), bbox.getMaximum(i));
                }
                header.bbox = new ImmutableEnvelope(e);
            }
        } else {
            header.bbox = new ImmutableEnvelope(new GeneralEnvelope(4));
        }

        channel.seek(0);
        header.write(channel);
        channel.flush();

        channel.channel.close();
    }

}
