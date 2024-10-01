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
public final class Position extends StructClass {

    private static final OfDouble LAYOUT_LON;
    private static final OfDouble LAYOUT_LAT;
    private static final OfDouble LAYOUT_Z;
    static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        LAYOUT_LON = GSF.C_DOUBLE.withName("lon"),
        LAYOUT_LAT = GSF.C_DOUBLE.withName("lat"),
        LAYOUT_Z = GSF.C_DOUBLE.withName("z")
    ).withName("t_gsf_gp");

    Position(MemorySegment segment) {
        super(segment);
    }

    Position(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    public double getLon() {
        return struct.get(LAYOUT_LON, 0);
    }

    public void setLon(double fieldValue) {
        struct.set(LAYOUT_LON, 0, fieldValue);
    }

    public double getLat() {
        return struct.get(LAYOUT_LAT, 8);
    }

    public void setLat(double fieldValue) {
        struct.set(LAYOUT_LAT, 8, fieldValue);
    }

    public double getZ() {
        return struct.get(LAYOUT_Z, 16);
    }

    public void setZ(double fieldValue) {
        struct.set(LAYOUT_Z, 16, fieldValue);
    }
}
