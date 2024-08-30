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
public final class SwathBathySummary extends StructClass{

    private static final GroupLayout LAYOUT_START_TIME;
    private static final GroupLayout LAYOUT_END_TIME;
    private static final OfDouble LAYOUT_MIN_LATITUDE;
    private static final OfDouble LAYOUT_MIN_LONGITUDE;
    private static final OfDouble LAYOUT_MAX_LATITUDE;
    private static final OfDouble LAYOUT_MAX_LONGITUDE;
    private static final OfDouble LAYOUT_MIN_DEPTH;
    private static final OfDouble LAYOUT_MAX_DEPTH;
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        LAYOUT_START_TIME = TimeSpec.LAYOUT.withName("start_time"),
        LAYOUT_END_TIME = TimeSpec.LAYOUT.withName("end_time"),
        LAYOUT_MIN_LATITUDE = GSF.C_DOUBLE.withName("min_latitude"),
        LAYOUT_MIN_LONGITUDE = GSF.C_DOUBLE.withName("min_longitude"),
        LAYOUT_MAX_LATITUDE = GSF.C_DOUBLE.withName("max_latitude"),
        LAYOUT_MAX_LONGITUDE = GSF.C_DOUBLE.withName("max_longitude"),
        LAYOUT_MIN_DEPTH = GSF.C_DOUBLE.withName("min_depth"),
        LAYOUT_MAX_DEPTH = GSF.C_DOUBLE.withName("max_depth")
    ).withName("t_gsfSwathBathySummary");

    public SwathBathySummary(MemorySegment struct) {
        super(struct);
    }

    public SwathBathySummary(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    public TimeSpec getStartTime() {
        return new TimeSpec(struct.asSlice(0, LAYOUT_START_TIME.byteSize()));
    }

    public TimeSpec getEndTime() {
        return new TimeSpec(struct.asSlice(16, LAYOUT_END_TIME.byteSize()));
    }

    public double getMinLatitude() {
        return struct.get(LAYOUT_MIN_LATITUDE, 32);
    }

    public double getMinLongitude() {
        return struct.get(LAYOUT_MIN_LONGITUDE, 40);
    }

    public double getMaxLatitude() {
        return struct.get(LAYOUT_MAX_LATITUDE, 48);
    }

    public double getMaxLongitude() {
        return struct.get(LAYOUT_MAX_LONGITUDE, 56);
    }

    public double getMinDepth() {
        return struct.get(LAYOUT_MIN_DEPTH, 64);
    }

    public double getMaxDepth() {
        return struct.get(LAYOUT_MAX_DEPTH, 72);
    }

}

