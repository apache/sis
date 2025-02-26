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
package org.apache.sis.storage.isobmff.image;

import java.io.IOException;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.base.EntityToGroup;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ImagePyramid extends EntityToGroup {

    public static final String FCC = "pymd";

    public static final class Matrix {
        public int layerBinning;
        public int tilesInLayerRowMinus1;
        public int tilesInLayerColumnMinus1;

        @Override
        public String toString() {
            return Box.beanToString(this);
        }
    }

    public int tileSizeX;
    public int tileSizeY;
    public Matrix[] matrices;

    @Override
    protected void readProperties(Reader reader) throws IOException {
        super.readProperties(reader);
        tileSizeX = reader.channel.readUnsignedShort();
        tileSizeY = reader.channel.readUnsignedShort();

        matrices = new Matrix[entitiesId.length];
        for (int i = 0; i < matrices.length; i++) {
            matrices[i] = new Matrix();
            matrices[i].layerBinning = reader.channel.readUnsignedShort();
            matrices[i].tilesInLayerRowMinus1 = reader.channel.readUnsignedShort();
            matrices[i].tilesInLayerColumnMinus1 = reader.channel.readUnsignedShort();
        }
    }

}
