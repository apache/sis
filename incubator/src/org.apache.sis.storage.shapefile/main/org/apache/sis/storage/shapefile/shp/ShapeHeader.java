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
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.ChannelDataOutput;
import org.opengis.geometry.Envelope;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * Shapefile header.
 * @author Johann Sorel (Geomatys)
 * @see <a href="http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf">ESRI Shapefile Specification</a>
 */
public final class ShapeHeader {

    /**
     * Constant header length.
     */
    public static final int HEADER_LENGTH = 100;
    /**
     * Shapefile header signature.
     */
    public static final int SIGNATURE = 9994;

    /**
     * File size, in bytes.
     */
    public int fileLength;
    /**
     * Shape type.
     */
    public int shapeType;
    /**
     * Shapefile bounding box without CRS.
     * Ordinates are in X,Y,Z,M order.
     */
    public Envelope bbox;

    /**
     * Read shapefile header.
     * @param channel input channel, not null
     * @throws IOException if an error occurred while reading.
     */
    public void read(final ChannelDataInput channel) throws IOException {
        final long position = channel.getStreamPosition();
        channel.buffer.order(ByteOrder.BIG_ENDIAN);
        //check signature
        if (channel.readInt() != SIGNATURE) {
            throw new IOException("Incorrect file signature");
        }
        //skip unused datas
        channel.skipBytes(5*4);
        fileLength = channel.readInt() * 2; //in 16bits words

        channel.buffer.order(ByteOrder.LITTLE_ENDIAN);
        final int version = channel.readInt();
        if (version != 1000) {
            throw new IOException("Incorrect file version, expected 1000 but was " + version);
        }
        shapeType = channel.readInt();
        final double[] bb = channel.readDoubles(8);
        GeneralEnvelope bbox = new GeneralEnvelope(4);
        bbox.setRange(0, bb[0], bb[2]);
        bbox.setRange(1, bb[1], bb[3]);
        bbox.setRange(2, bb[4], bb[5]);
        bbox.setRange(3, bb[6], bb[7]);
        this.bbox = new ImmutableEnvelope(bbox);
    }

    /**
     * Write shapefile header.
     * @param channel output channel, not null
     * @throws IOException if an error occurred while writing.
     */
    public void write(ChannelDataOutput channel) throws IOException {
        channel.buffer.order(ByteOrder.BIG_ENDIAN);
        channel.writeInt(SIGNATURE);
        channel.write(new byte[5*4]);
        channel.writeInt(fileLength/2);
        channel.buffer.order(ByteOrder.LITTLE_ENDIAN);
        channel.writeInt(1000);
        channel.writeInt(shapeType);
        channel.writeDouble(bbox.getMinimum(0));
        channel.writeDouble(bbox.getMinimum(1));
        channel.writeDouble(bbox.getMaximum(0));
        channel.writeDouble(bbox.getMaximum(1));
        channel.writeDouble(bbox.getMinimum(2));
        channel.writeDouble(bbox.getMaximum(2));
        channel.writeDouble(bbox.getMinimum(3));
        channel.writeDouble(bbox.getMaximum(3));
        channel.flush();
    }

}
