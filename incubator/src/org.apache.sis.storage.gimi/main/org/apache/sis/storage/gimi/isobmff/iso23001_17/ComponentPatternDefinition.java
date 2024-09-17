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
import org.apache.sis.storage.gimi.isobmff.FullBox;
import org.apache.sis.storage.gimi.isobmff.ISOBMFFReader;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ComponentPatternDefinition extends FullBox{

    public static final String FCC = "cpat";

    public int[][] patternComponentIndex;
    public double[][] patternComponentGain;

    @Override
    public void readProperties(ISOBMFFReader reader) throws IOException {

        final int width = reader.channel.readUnsignedShort();
        final int height = reader.channel.readUnsignedShort();
        patternComponentIndex = new int[height][width];
        patternComponentGain = new double[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                patternComponentIndex[y][x] = reader.channel.readInt();
                patternComponentGain[y][x] = reader.channel.readDouble();
            }
        }
    }

}