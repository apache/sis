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
package org.apache.sis.storage.gsf;

import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ScaleFactors extends StructClass{

    private static final OfInt LAYOUT_NUMARRAYSUBRECORDS;
    private static final SequenceLayout LAYOUT_SCALETABLE;
    static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        LAYOUT_NUMARRAYSUBRECORDS = GSF.C_INT.withName("numArraySubrecords"),
        MemoryLayout.paddingLayout(4),
        LAYOUT_SCALETABLE = MemoryLayout.sequenceLayout(30, ScaleInfo.LAYOUT).withName("scaleTable")
    ).withName("t_gsfScaleFactors");


    ScaleFactors(MemorySegment struct) {
        super(struct);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int numArraySubrecords
     * }
     */
    public int getNumArraySubrecords(){
        return struct.get(LAYOUT_NUMARRAYSUBRECORDS, 0);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * gsfScaleInfo scaleTable[30]
     * }
     */
    public ScaleInfo[] getScaleTables() {
        return getObjects(8, 30, ScaleInfo.class);
    }
}
