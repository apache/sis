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
 * distributed under the License is distributed on an "AS IS" BASIS,z
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.isobmff.mpeg;

import java.io.IOException;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class UncompressedFrameConfig extends FullBox {

    public static final String FCC = "uncC";

    public static final class Component {
        public int componentIndex;
        public int componentBitDepthMinusOne;
        public int componentFormat;
        public int componentAlignSize;
        @Override
        public String toString() {
            return Box.beanToString(this);
        }
    }

    public String profile;
    public Component[] components;
    public int samplingType;
    public int interleaveType;
    public int blockSize;
    public boolean componentsLittleEndian;
    public boolean blockPadLsb;
    public boolean blockLittleEndian;
    public boolean blockReserved;
    public boolean padUnknown;
    public int pixelSize;
    public int rowAlignSize;
    public int tileAlignSize;
    public int numTileColsMinusOne;
    public int numTileRowsMinusOne;

    @Override
    public void readProperties(Reader reader) throws IOException {
        profile = intToFourCC(reader.channel.readInt());
        if (version == 1) {
            throw new IOException("TODO");
        } else if (version == 0) {
            components = new Component[reader.channel.readInt()];
            for (int i = 0; i < components.length; i++) {
                components[i] = new Component();
                components[i].componentIndex = reader.channel.readUnsignedShort();
                components[i].componentBitDepthMinusOne = reader.channel.readUnsignedByte();
                components[i].componentFormat = reader.channel.readUnsignedByte();
                components[i].componentAlignSize = reader.channel.readUnsignedByte();
            }
            samplingType = reader.channel.readUnsignedByte();
            interleaveType = reader.channel.readUnsignedByte();
            blockSize = reader.channel.readUnsignedByte();
            componentsLittleEndian = reader.channel.readBit() == 1;
            blockPadLsb = reader.channel.readBit() == 1;
            blockLittleEndian = reader.channel.readBit() == 1;
            blockReserved = reader.channel.readBit() == 1;
            padUnknown = reader.channel.readBit() == 1;
            reader.channel.readBits(3);
            pixelSize = reader.channel.readInt();
            rowAlignSize = reader.channel.readInt();
            tileAlignSize = reader.channel.readInt();
            numTileColsMinusOne = reader.channel.readInt();
            numTileRowsMinusOne = reader.channel.readInt();
        } else {
            throw new IOException("Unsupported version " + version);
        }
    }

}
