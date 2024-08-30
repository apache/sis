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
public final class ScaleInfo extends StructClass {

    private static final OfByte LAYOUT_COMPRESSIONFLAG;
    private static final OfDouble LAYOUT_MULTIPLIER;
    private static final OfDouble LAYOUT_OFFSET;

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        LAYOUT_COMPRESSIONFLAG = GSF.C_CHAR.withName("compressionFlag"),
        MemoryLayout.paddingLayout(7),
        LAYOUT_MULTIPLIER = GSF.C_DOUBLE.withName("multiplier"),
        LAYOUT_OFFSET = GSF.C_DOUBLE.withName("offset")
    ).withName("t_gsfScaleInfo");


    public ScaleInfo(MemorySegment struct) {
        super(struct);
    }

    public ScaleInfo(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char compressionFlag
     * }
     */
    public byte getCompressionFlag() {
        return struct.get(LAYOUT_COMPRESSIONFLAG, 0);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double multiplier
     * }
     */
    public double getMultiplier() {
        return struct.get(LAYOUT_MULTIPLIER, 8);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double offset
     * }
     */
    public double getOffset() {
        return struct.get(LAYOUT_OFFSET, 16);
    }

}

