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
public final class PositionOffsets extends StructClass {

    private static final OfDouble LAYOUT_X;
    private static final OfDouble LAYOUT_Y;
    private static final OfDouble LAYOUT_Z;
    private static final long x$OFFSET = 0;
    private static final long y$OFFSET = 8;
    private static final long z$OFFSET = 16;

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        LAYOUT_X = GSF.C_DOUBLE.withName("x"),
        LAYOUT_Y = GSF.C_DOUBLE.withName("y"),
        LAYOUT_Z = GSF.C_DOUBLE.withName("z")
    ).withName("t_gsf_pos_offsets");

    public PositionOffsets(MemorySegment struct) {
        super(struct);
    }

    public PositionOffsets(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double x
     * }
     */
    public double getX() {
        return struct.get(LAYOUT_X, x$OFFSET);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double y
     * }
     */
    public double getY() {
        return struct.get(LAYOUT_Y, y$OFFSET);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double z
     * }
     */
    public double getZ() {
        return struct.get(LAYOUT_Z, z$OFFSET);
    }

}

