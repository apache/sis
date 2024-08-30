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
public final class SVP extends StructClass {

    private static final GroupLayout OBSERVATION_TIMELAYOUT;
    private static final GroupLayout APPLICATION_TIMELAYOUT;
    private static final OfDouble LAYOUT_LATITUDE;
    private static final OfDouble LAYOUT_LONGITUDE;
    private static final OfInt LAYOUT_NUMBER_POINTS;
    private static final AddressLayout LAYOUT_DEPTH;
    private static final AddressLayout LAYOUT_SOUND_SPEED;
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        OBSERVATION_TIMELAYOUT = TimeSpec.LAYOUT.withName("observation_time"),
        APPLICATION_TIMELAYOUT = TimeSpec.LAYOUT.withName("application_time"),
        LAYOUT_LATITUDE = GSF.C_DOUBLE.withName("latitude"),
        LAYOUT_LONGITUDE = GSF.C_DOUBLE.withName("longitude"),
        LAYOUT_NUMBER_POINTS = GSF.C_INT.withName("number_points"),
        MemoryLayout.paddingLayout(4),
        LAYOUT_DEPTH = GSF.C_POINTER.withName("depth"),
        LAYOUT_SOUND_SPEED = GSF.C_POINTER.withName("sound_speed")
    ).withName("t_gsfSVP");

    public SVP(MemorySegment struct) {
        super(struct);
    }

    public SVP(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    public TimeSpec getObservationTime() {
        return new TimeSpec(struct.asSlice(0, OBSERVATION_TIMELAYOUT.byteSize()));
    }

    public TimeSpec getApplicationTime() {
        return new TimeSpec(struct.asSlice(16, APPLICATION_TIMELAYOUT.byteSize()));
    }

    public double getLatitude() {
        return struct.get(LAYOUT_LATITUDE, 32);
    }

    public double getLongitude() {
        return struct.get(LAYOUT_LONGITUDE, 40);
    }

    public int getNumberPoints() {
        return struct.get(LAYOUT_NUMBER_POINTS, 48);
    }

    public double[] getDepth() {
        final MemorySegment resolvedAddress = struct.get(LAYOUT_DEPTH, 56);
        return getDoubles(resolvedAddress, 0, getNumberPoints());
    }

    public double[] getSoundSpeed() {
        final MemorySegment resolvedAddress = struct.get(LAYOUT_SOUND_SPEED, 64);
        return getDoubles(resolvedAddress, 0, getNumberPoints());
    }

}
