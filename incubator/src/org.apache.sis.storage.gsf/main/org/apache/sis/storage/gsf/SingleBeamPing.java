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
public final class SingleBeamPing extends StructClass {

    private static final GroupLayout LAYOUT_PING_TIME;
    private static final OfDouble LAYOUT_LATITUDE;
    private static final OfDouble LAYOUT_LONGITUDE;
    private static final OfDouble LAYOUT_TIDE_CORRECTOR;
    private static final OfDouble LAYOUT_DEPTH_CORRECTOR;
    private static final OfDouble LAYOUT_HEADING;
    private static final OfDouble LAYOUT_PITCH;
    private static final OfDouble LAYOUT_ROLL;
    private static final OfDouble LAYOUT_HEAVE;
    private static final OfDouble LAYOUT_DEPTH;
    private static final OfDouble LAYOUT_SOUND_SPEED_CORRECTION;
    private static final OfShort LAYOUT_POSITIONING_SYSTEM_TYPE;
    private static final OfInt LAYOUT_SENSOR_ID;
    private static final GroupLayout LAYOUT_SENSOR_DATA;

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        LAYOUT_PING_TIME = TimeSpec.LAYOUT.withName("ping_time"),
        LAYOUT_LATITUDE = GSF.C_DOUBLE.withName("latitude"),
        LAYOUT_LONGITUDE = GSF.C_DOUBLE.withName("longitude"),
        LAYOUT_TIDE_CORRECTOR = GSF.C_DOUBLE.withName("tide_corrector"),
        LAYOUT_DEPTH_CORRECTOR = GSF.C_DOUBLE.withName("depth_corrector"),
        LAYOUT_HEADING = GSF.C_DOUBLE.withName("heading"),
        LAYOUT_PITCH = GSF.C_DOUBLE.withName("pitch"),
        LAYOUT_ROLL = GSF.C_DOUBLE.withName("roll"),
        LAYOUT_HEAVE = GSF.C_DOUBLE.withName("heave"),
        LAYOUT_DEPTH = GSF.C_DOUBLE.withName("depth"),
        LAYOUT_SOUND_SPEED_CORRECTION = GSF.C_DOUBLE.withName("sound_speed_correction"),
        LAYOUT_POSITIONING_SYSTEM_TYPE = GSF.C_SHORT.withName("positioning_system_type"),
        MemoryLayout.paddingLayout(2),
        LAYOUT_SENSOR_ID = GSF.C_INT.withName("sensor_id"),
        LAYOUT_SENSOR_DATA = SBSensorSpecific.LAYOUT.withName("sensor_data")
    ).withName("t_gsfSingleBeamPing");

    public SingleBeamPing(MemorySegment struct) {
        super(struct);
    }

    public SingleBeamPing(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * struct timespec ping_time
     * }
     */
    public TimeSpec getPingTime() {
        return new TimeSpec(struct.asSlice(0, LAYOUT_PING_TIME.byteSize()));
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double latitude
     * }
     */
    public double getLatitude() {
        return struct.get(LAYOUT_LATITUDE, 16);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double longitude
     * }
     */
    public double getLongitude() {
        return struct.get(LAYOUT_LONGITUDE, 24);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tide_corrector
     * }
     */
    public double getTideCorrector() {
        return struct.get(LAYOUT_TIDE_CORRECTOR, 32);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double depth_corrector
     * }
     */
    public double getDepthCorrector() {
        return struct.get(LAYOUT_DEPTH_CORRECTOR, 40);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double heading
     * }
     */
    public double getHeading() {
        return struct.get(LAYOUT_HEADING, 48);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double pitch
     * }
     */
    public double getPitch() {
        return struct.get(LAYOUT_PITCH, 56);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double roll
     * }
     */
    public double getRoll() {
        return struct.get(LAYOUT_ROLL, 64);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double heave
     * }
     */
    public double getHeave() {
        return struct.get(LAYOUT_HEAVE, 72);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double depth
     * }
     */
    public double getDepth() {
        return struct.get(LAYOUT_DEPTH, 80);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double sound_speed_correction
     * }
     */
    public double getSoundSpeedCorrection() {
        return struct.get(LAYOUT_SOUND_SPEED_CORRECTION, 88);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short positioning_system_type
     * }
     */
    public short getPositioningSystemType() {
        return struct.get(LAYOUT_POSITIONING_SYSTEM_TYPE, 96);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int sensor_id
     * }
     */
    public int getSensorId() {
        return struct.get(LAYOUT_SENSOR_ID, 100);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * gsfSBSensorSpecific sensor_data
     * }
     */
    public MemorySegment getSensorData() {
        return struct.asSlice(104, LAYOUT_SENSOR_DATA.byteSize());
    }

}

