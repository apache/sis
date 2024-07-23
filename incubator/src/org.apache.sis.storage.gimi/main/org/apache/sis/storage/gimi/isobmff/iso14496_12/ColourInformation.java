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
package org.apache.sis.storage.gimi.isobmff.iso14496_12;

import java.io.IOException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.gimi.isobmff.Box;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ColourInformation extends Box{

    public static final String FCC = "colr";

    public String colourType;
    public int colourPrimaries;
    public int transferCharacteristics;
    public int matrixCoefficients;
    public boolean fullRangeFlag;

    @Override
    protected void readProperties(ChannelDataInput cdi) throws IOException {
        colourType = Box.intToFourCC(cdi.readInt());
        if ("nclx".equals(colourType)) {
            colourPrimaries = cdi.readUnsignedShort();
            transferCharacteristics = cdi.readUnsignedShort();
            matrixCoefficients = cdi.readUnsignedShort();
            fullRangeFlag = cdi.readBit() == 1;
            cdi.skipRemainingBits();
        } else if ("rICC".equals(colourType)) {
            throw new IOException();
        } else if ("prof".equals(colourType)) {
            throw new IOException();
        }
    }



}
