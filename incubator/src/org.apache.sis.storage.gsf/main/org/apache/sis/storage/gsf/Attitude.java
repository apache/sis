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
public final class Attitude extends StructClass {

    private static final OfShort LAYOUT_NUM_MEASUREMENTS;
    private static final AddressLayout LAYOUT_ATTITUDE_TIME;
    private static final AddressLayout LAYOUT_PITCH;
    private static final AddressLayout LAYOUT_ROLL;
    private static final AddressLayout LAYOUT_HEAVE;
    private static final AddressLayout LAYOUT_HEADING;
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        LAYOUT_NUM_MEASUREMENTS = GSF.C_SHORT.withName("num_measurements"),
        MemoryLayout.paddingLayout(6),
        LAYOUT_ATTITUDE_TIME = GSF.C_POINTER.withName("attitude_time"),
        LAYOUT_PITCH = GSF.C_POINTER.withName("pitch"),
        LAYOUT_ROLL = GSF.C_POINTER.withName("roll"),
        LAYOUT_HEAVE = GSF.C_POINTER.withName("heave"),
        LAYOUT_HEADING = GSF.C_POINTER.withName("heading")
    ).withName("t_gsfAttitude");

    public Attitude(MemorySegment struct) {
        super(struct);
    }

    public Attitude(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }


    public int getNumMeasurements() {
        return struct.get(LAYOUT_NUM_MEASUREMENTS, 0);
    }

    public int[] getAttitudeTime() {
        final MemorySegment resolvedAddress = struct.get(LAYOUT_ATTITUDE_TIME, 8);
        return getInts(resolvedAddress, 0, getNumMeasurements());
    }

    public double[] getPitch() {
        final MemorySegment resolvedAddress = struct.get(LAYOUT_PITCH, 16);
        return getDoubles(resolvedAddress, 0, getNumMeasurements());
    }

    public double[] getRoll() {
        final MemorySegment resolvedAddress = struct.get(LAYOUT_ROLL, 24);
        return getDoubles(resolvedAddress, 0, getNumMeasurements());
    }

    public double[] getHeave() {
        final MemorySegment resolvedAddress = struct.get(LAYOUT_HEAVE, 32);
        return getDoubles(resolvedAddress, 0, getNumMeasurements());
    }

    public double[] getHeading() {
        final MemorySegment resolvedAddress = struct.get(LAYOUT_HEADING, 40);
        return getDoubles(resolvedAddress, 0, getNumMeasurements());
    }

}

