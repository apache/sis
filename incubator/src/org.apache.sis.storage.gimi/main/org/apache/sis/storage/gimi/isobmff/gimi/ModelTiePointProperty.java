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
package org.apache.sis.storage.gimi.isobmff.gimi;

import java.io.IOException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.gimi.isobmff.Box;
import org.apache.sis.storage.gimi.isobmff.iso14496_12.ItemFullProperty;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ModelTiePointProperty extends ItemFullProperty {

    public static final String UUID = "c683364f-d6a4-48b8-a76b-17a30af40c10";

    public static class TiePoint {

        public int[] ijk;
        public double[] xyz;

        @Override
        public String toString() {
            return Box.beanToString(this);
        }
    }

    public TiePoint[] tiepoints;

    @Override
    protected void readProperties(ChannelDataInput cdi) throws IOException {
        tiepoints = new TiePoint[cdi.readUnsignedShort()];
        for (int i = 0; i < tiepoints.length; i++) {
            tiepoints[i] = new TiePoint();
            if ((flags & 0x01) == 1) {
                //2D
                tiepoints[i].ijk = cdi.readInts(2);
                tiepoints[i].xyz = cdi.readDoubles(2);
            } else {
                //3D
                tiepoints[i].ijk = cdi.readInts(3);
                tiepoints[i].xyz = cdi.readDoubles(3);
            }
        }
    }


}
