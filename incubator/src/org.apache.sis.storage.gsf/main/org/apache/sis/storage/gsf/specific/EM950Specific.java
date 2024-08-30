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
package org.apache.sis.storage.gsf.specific;

import java.lang.foreign.*;

import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.MemoryLayout.PathElement.*;
import org.apache.sis.storage.gsf.GSF;
import org.apache.sis.storage.gsf.StructClass;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class EM950Specific extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("ping_number"),
        GSF.C_INT.withName("mode"),
        GSF.C_INT.withName("ping_quality"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("ship_pitch"),
        GSF.C_DOUBLE.withName("transducer_pitch"),
        GSF.C_DOUBLE.withName("surface_velocity")
    ).withName("t_gsfEM950Specific");

    public EM950Specific(MemorySegment struct) {
        super(struct);
    }

    public EM950Specific(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt ping_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_number"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int ping_number
     * }
     */
    public static final OfInt ping_numberLAYOUT() {
        return ping_numberLAYOUT;
    }

    private static final long ping_number$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int ping_number
     * }
     */
    public static final long ping_number$offset() {
        return ping_number$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_number
     * }
     */
    public static int ping_number(MemorySegment struct) {
        return struct.get(ping_numberLAYOUT, ping_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_number
     * }
     */
    public static void ping_number(MemorySegment struct, int fieldValue) {
        struct.set(ping_numberLAYOUT, ping_number$OFFSET, fieldValue);
    }

    private static final OfInt modeLAYOUT = (OfInt)LAYOUT.select(groupElement("mode"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int mode
     * }
     */
    public static final OfInt modeLAYOUT() {
        return modeLAYOUT;
    }

    private static final long mode$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int mode
     * }
     */
    public static final long mode$offset() {
        return mode$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int mode
     * }
     */
    public static int mode(MemorySegment struct) {
        return struct.get(modeLAYOUT, mode$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int mode
     * }
     */
    public static void mode(MemorySegment struct, int fieldValue) {
        struct.set(modeLAYOUT, mode$OFFSET, fieldValue);
    }

    private static final OfInt ping_qualityLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_quality"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int ping_quality
     * }
     */
    public static final OfInt ping_qualityLAYOUT() {
        return ping_qualityLAYOUT;
    }

    private static final long ping_quality$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int ping_quality
     * }
     */
    public static final long ping_quality$offset() {
        return ping_quality$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_quality
     * }
     */
    public static int ping_quality(MemorySegment struct) {
        return struct.get(ping_qualityLAYOUT, ping_quality$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_quality
     * }
     */
    public static void ping_quality(MemorySegment struct, int fieldValue) {
        struct.set(ping_qualityLAYOUT, ping_quality$OFFSET, fieldValue);
    }

    private static final OfDouble ship_pitchLAYOUT = (OfDouble)LAYOUT.select(groupElement("ship_pitch"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double ship_pitch
     * }
     */
    public static final OfDouble ship_pitchLAYOUT() {
        return ship_pitchLAYOUT;
    }

    private static final long ship_pitch$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double ship_pitch
     * }
     */
    public static final long ship_pitch$offset() {
        return ship_pitch$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double ship_pitch
     * }
     */
    public static double ship_pitch(MemorySegment struct) {
        return struct.get(ship_pitchLAYOUT, ship_pitch$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double ship_pitch
     * }
     */
    public static void ship_pitch(MemorySegment struct, double fieldValue) {
        struct.set(ship_pitchLAYOUT, ship_pitch$OFFSET, fieldValue);
    }

    private static final OfDouble transducer_pitchLAYOUT = (OfDouble)LAYOUT.select(groupElement("transducer_pitch"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double transducer_pitch
     * }
     */
    public static final OfDouble transducer_pitchLAYOUT() {
        return transducer_pitchLAYOUT;
    }

    private static final long transducer_pitch$OFFSET = 24;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double transducer_pitch
     * }
     */
    public static final long transducer_pitch$offset() {
        return transducer_pitch$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double transducer_pitch
     * }
     */
    public static double transducer_pitch(MemorySegment struct) {
        return struct.get(transducer_pitchLAYOUT, transducer_pitch$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double transducer_pitch
     * }
     */
    public static void transducer_pitch(MemorySegment struct, double fieldValue) {
        struct.set(transducer_pitchLAYOUT, transducer_pitch$OFFSET, fieldValue);
    }

    private static final OfDouble surface_velocityLAYOUT = (OfDouble)LAYOUT.select(groupElement("surface_velocity"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public static final OfDouble surface_velocityLAYOUT() {
        return surface_velocityLAYOUT;
    }

    private static final long surface_velocity$OFFSET = 32;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public static final long surface_velocity$offset() {
        return surface_velocity$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public static double surface_velocity(MemorySegment struct) {
        return struct.get(surface_velocityLAYOUT, surface_velocity$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public static void surface_velocity(MemorySegment struct, double fieldValue) {
        struct.set(surface_velocityLAYOUT, surface_velocity$OFFSET, fieldValue);
    }

}

