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

    public ShapeWriter(ChannelDataOutput channel) throws IOException {
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
        this.header.bbox = new ImmutableEnvelope(new GeneralEnvelope(4));
        header.write(channel);
        io = ShapeGeometryEncoder.getEncoder(header.shapeType);
    }

    public void write(ShapeRecord record) throws IOException {
        record.write(channel, io);
        if (bbox == null) {
            bbox = new GeneralEnvelope(record.bbox.getDimension());
            bbox.setEnvelope(record.bbox);
        } else {
            bbox.add(record.bbox);
        }
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
