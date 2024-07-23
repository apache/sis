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
package org.apache.sis.storage.gimi.isobmff.iso23001_17;

import java.io.IOException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.gimi.isobmff.Box;
import org.apache.sis.storage.gimi.isobmff.FullBox;

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
    public void readProperties(ChannelDataInput cdi) throws IOException {
        profile = intToFourCC(cdi.readInt());
        if (version == 1) {
            throw new IOException("TODO");
        } else if (version == 0) {
            components = new Component[cdi.readInt()];
            for (int i = 0; i < components.length; i++) {
                components[i] = new Component();
                components[i].componentIndex = cdi.readUnsignedShort();
                components[i].componentBitDepthMinusOne = cdi.readUnsignedByte();
                components[i].componentFormat = cdi.readUnsignedByte();
                components[i].componentAlignSize = cdi.readUnsignedByte();
            }
            samplingType = cdi.readUnsignedByte();
            interleaveType = cdi.readUnsignedByte();
            blockSize = cdi.readUnsignedByte();
            componentsLittleEndian = cdi.readBit() == 1;
            blockPadLsb = cdi.readBit() == 1;
            blockLittleEndian = cdi.readBit() == 1;
            blockReserved = cdi.readBit() == 1;
            padUnknown = cdi.readBit() == 1;
            cdi.readBits(3);
            pixelSize = cdi.readInt();
            rowAlignSize = cdi.readInt();
            tileAlignSize = cdi.readInt();
            numTileColsMinusOne = cdi.readInt();
            numTileRowsMinusOne = cdi.readInt();
        } else {
            throw new IOException("Unsupported version " + version);
        }
    }

}
