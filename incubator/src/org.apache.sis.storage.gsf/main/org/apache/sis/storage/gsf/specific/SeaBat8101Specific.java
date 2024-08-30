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

import java.lang.invoke.*;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.MemoryLayout.PathElement.*;
import org.apache.sis.storage.gsf.GSF;
import org.apache.sis.storage.gsf.StructClass;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SeaBat8101Specific extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("ping_number"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("surface_velocity"),
        GSF.C_INT.withName("mode"),
        GSF.C_INT.withName("range"),
        GSF.C_INT.withName("power"),
        GSF.C_INT.withName("gain"),
        GSF.C_INT.withName("pulse_width"),
        GSF.C_INT.withName("tvg_spreading"),
        GSF.C_INT.withName("tvg_absorption"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("fore_aft_bw"),
        GSF.C_DOUBLE.withName("athwart_bw"),
        GSF.C_DOUBLE.withName("range_filt_min"),
        GSF.C_DOUBLE.withName("range_filt_max"),
        GSF.C_DOUBLE.withName("depth_filt_min"),
        GSF.C_DOUBLE.withName("depth_filt_max"),
        GSF.C_INT.withName("projector"),
        MemoryLayout.sequenceLayout(4, GSF.C_CHAR).withName("spare")
    ).withName("t_gsfSeaBat8101Specific");

    public SeaBat8101Specific(MemorySegment struct) {
        super(struct);
    }

    public SeaBat8101Specific(SegmentAllocator allocator) {
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

    private static final long surface_velocity$OFFSET = 8;

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

    private static final long mode$OFFSET = 16;

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

    private static final OfInt rangeLAYOUT = (OfInt)LAYOUT.select(groupElement("range"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int range
     * }
     */
    public static final OfInt rangeLAYOUT() {
        return rangeLAYOUT;
    }

    private static final long range$OFFSET = 20;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int range
     * }
     */
    public static final long range$offset() {
        return range$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int range
     * }
     */
    public static int range(MemorySegment struct) {
        return struct.get(rangeLAYOUT, range$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int range
     * }
     */
    public static void range(MemorySegment struct, int fieldValue) {
        struct.set(rangeLAYOUT, range$OFFSET, fieldValue);
    }

    private static final OfInt powerLAYOUT = (OfInt)LAYOUT.select(groupElement("power"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int power
     * }
     */
    public static final OfInt powerLAYOUT() {
        return powerLAYOUT;
    }

    private static final long power$OFFSET = 24;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int power
     * }
     */
    public static final long power$offset() {
        return power$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int power
     * }
     */
    public static int power(MemorySegment struct) {
        return struct.get(powerLAYOUT, power$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int power
     * }
     */
    public static void power(MemorySegment struct, int fieldValue) {
        struct.set(powerLAYOUT, power$OFFSET, fieldValue);
    }

    private static final OfInt gainLAYOUT = (OfInt)LAYOUT.select(groupElement("gain"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int gain
     * }
     */
    public static final OfInt gainLAYOUT() {
        return gainLAYOUT;
    }

    private static final long gain$OFFSET = 28;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int gain
     * }
     */
    public static final long gain$offset() {
        return gain$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int gain
     * }
     */
    public static int gain(MemorySegment struct) {
        return struct.get(gainLAYOUT, gain$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int gain
     * }
     */
    public static void gain(MemorySegment struct, int fieldValue) {
        struct.set(gainLAYOUT, gain$OFFSET, fieldValue);
    }

    private static final OfInt pulse_widthLAYOUT = (OfInt)LAYOUT.select(groupElement("pulse_width"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int pulse_width
     * }
     */
    public static final OfInt pulse_widthLAYOUT() {
        return pulse_widthLAYOUT;
    }

    private static final long pulse_width$OFFSET = 32;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int pulse_width
     * }
     */
    public static final long pulse_width$offset() {
        return pulse_width$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int pulse_width
     * }
     */
    public static int pulse_width(MemorySegment struct) {
        return struct.get(pulse_widthLAYOUT, pulse_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int pulse_width
     * }
     */
    public static void pulse_width(MemorySegment struct, int fieldValue) {
        struct.set(pulse_widthLAYOUT, pulse_width$OFFSET, fieldValue);
    }

    private static final OfInt tvg_spreadingLAYOUT = (OfInt)LAYOUT.select(groupElement("tvg_spreading"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int tvg_spreading
     * }
     */
    public static final OfInt tvg_spreadingLAYOUT() {
        return tvg_spreadingLAYOUT;
    }

    private static final long tvg_spreading$OFFSET = 36;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int tvg_spreading
     * }
     */
    public static final long tvg_spreading$offset() {
        return tvg_spreading$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int tvg_spreading
     * }
     */
    public static int tvg_spreading(MemorySegment struct) {
        return struct.get(tvg_spreadingLAYOUT, tvg_spreading$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int tvg_spreading
     * }
     */
    public static void tvg_spreading(MemorySegment struct, int fieldValue) {
        struct.set(tvg_spreadingLAYOUT, tvg_spreading$OFFSET, fieldValue);
    }

    private static final OfInt tvg_absorptionLAYOUT = (OfInt)LAYOUT.select(groupElement("tvg_absorption"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int tvg_absorption
     * }
     */
    public static final OfInt tvg_absorptionLAYOUT() {
        return tvg_absorptionLAYOUT;
    }

    private static final long tvg_absorption$OFFSET = 40;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int tvg_absorption
     * }
     */
    public static final long tvg_absorption$offset() {
        return tvg_absorption$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int tvg_absorption
     * }
     */
    public static int tvg_absorption(MemorySegment struct) {
        return struct.get(tvg_absorptionLAYOUT, tvg_absorption$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int tvg_absorption
     * }
     */
    public static void tvg_absorption(MemorySegment struct, int fieldValue) {
        struct.set(tvg_absorptionLAYOUT, tvg_absorption$OFFSET, fieldValue);
    }

    private static final OfDouble fore_aft_bwLAYOUT = (OfDouble)LAYOUT.select(groupElement("fore_aft_bw"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double fore_aft_bw
     * }
     */
    public static final OfDouble fore_aft_bwLAYOUT() {
        return fore_aft_bwLAYOUT;
    }

    private static final long fore_aft_bw$OFFSET = 48;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double fore_aft_bw
     * }
     */
    public static final long fore_aft_bw$offset() {
        return fore_aft_bw$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double fore_aft_bw
     * }
     */
    public static double fore_aft_bw(MemorySegment struct) {
        return struct.get(fore_aft_bwLAYOUT, fore_aft_bw$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double fore_aft_bw
     * }
     */
    public static void fore_aft_bw(MemorySegment struct, double fieldValue) {
        struct.set(fore_aft_bwLAYOUT, fore_aft_bw$OFFSET, fieldValue);
    }

    private static final OfDouble athwart_bwLAYOUT = (OfDouble)LAYOUT.select(groupElement("athwart_bw"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double athwart_bw
     * }
     */
    public static final OfDouble athwart_bwLAYOUT() {
        return athwart_bwLAYOUT;
    }

    private static final long athwart_bw$OFFSET = 56;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double athwart_bw
     * }
     */
    public static final long athwart_bw$offset() {
        return athwart_bw$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double athwart_bw
     * }
     */
    public static double athwart_bw(MemorySegment struct) {
        return struct.get(athwart_bwLAYOUT, athwart_bw$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double athwart_bw
     * }
     */
    public static void athwart_bw(MemorySegment struct, double fieldValue) {
        struct.set(athwart_bwLAYOUT, athwart_bw$OFFSET, fieldValue);
    }

    private static final OfDouble range_filt_minLAYOUT = (OfDouble)LAYOUT.select(groupElement("range_filt_min"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double range_filt_min
     * }
     */
    public static final OfDouble range_filt_minLAYOUT() {
        return range_filt_minLAYOUT;
    }

    private static final long range_filt_min$OFFSET = 64;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double range_filt_min
     * }
     */
    public static final long range_filt_min$offset() {
        return range_filt_min$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double range_filt_min
     * }
     */
    public static double range_filt_min(MemorySegment struct) {
        return struct.get(range_filt_minLAYOUT, range_filt_min$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double range_filt_min
     * }
     */
    public static void range_filt_min(MemorySegment struct, double fieldValue) {
        struct.set(range_filt_minLAYOUT, range_filt_min$OFFSET, fieldValue);
    }

    private static final OfDouble range_filt_maxLAYOUT = (OfDouble)LAYOUT.select(groupElement("range_filt_max"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double range_filt_max
     * }
     */
    public static final OfDouble range_filt_maxLAYOUT() {
        return range_filt_maxLAYOUT;
    }

    private static final long range_filt_max$OFFSET = 72;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double range_filt_max
     * }
     */
    public static final long range_filt_max$offset() {
        return range_filt_max$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double range_filt_max
     * }
     */
    public static double range_filt_max(MemorySegment struct) {
        return struct.get(range_filt_maxLAYOUT, range_filt_max$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double range_filt_max
     * }
     */
    public static void range_filt_max(MemorySegment struct, double fieldValue) {
        struct.set(range_filt_maxLAYOUT, range_filt_max$OFFSET, fieldValue);
    }

    private static final OfDouble depth_filt_minLAYOUT = (OfDouble)LAYOUT.select(groupElement("depth_filt_min"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double depth_filt_min
     * }
     */
    public static final OfDouble depth_filt_minLAYOUT() {
        return depth_filt_minLAYOUT;
    }

    private static final long depth_filt_min$OFFSET = 80;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double depth_filt_min
     * }
     */
    public static final long depth_filt_min$offset() {
        return depth_filt_min$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double depth_filt_min
     * }
     */
    public static double depth_filt_min(MemorySegment struct) {
        return struct.get(depth_filt_minLAYOUT, depth_filt_min$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double depth_filt_min
     * }
     */
    public static void depth_filt_min(MemorySegment struct, double fieldValue) {
        struct.set(depth_filt_minLAYOUT, depth_filt_min$OFFSET, fieldValue);
    }

    private static final OfDouble depth_filt_maxLAYOUT = (OfDouble)LAYOUT.select(groupElement("depth_filt_max"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double depth_filt_max
     * }
     */
    public static final OfDouble depth_filt_maxLAYOUT() {
        return depth_filt_maxLAYOUT;
    }

    private static final long depth_filt_max$OFFSET = 88;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double depth_filt_max
     * }
     */
    public static final long depth_filt_max$offset() {
        return depth_filt_max$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double depth_filt_max
     * }
     */
    public static double depth_filt_max(MemorySegment struct) {
        return struct.get(depth_filt_maxLAYOUT, depth_filt_max$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double depth_filt_max
     * }
     */
    public static void depth_filt_max(MemorySegment struct, double fieldValue) {
        struct.set(depth_filt_maxLAYOUT, depth_filt_max$OFFSET, fieldValue);
    }

    private static final OfInt projectorLAYOUT = (OfInt)LAYOUT.select(groupElement("projector"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int projector
     * }
     */
    public static final OfInt projectorLAYOUT() {
        return projectorLAYOUT;
    }

    private static final long projector$OFFSET = 96;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int projector
     * }
     */
    public static final long projector$offset() {
        return projector$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int projector
     * }
     */
    public static int projector(MemorySegment struct) {
        return struct.get(projectorLAYOUT, projector$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int projector
     * }
     */
    public static void projector(MemorySegment struct, int fieldValue) {
        struct.set(projectorLAYOUT, projector$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public static final SequenceLayout spareLAYOUT() {
        return spareLAYOUT;
    }

    private static final long spare$OFFSET = 100;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public static final long spare$offset() {
        return spare$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public static MemorySegment spare(MemorySegment struct) {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public static void spare(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static long[] spare$DIMS = { 4 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public static long[] spare$dimensions() {
        return spare$DIMS;
    }
    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public static byte spare(MemorySegment struct, long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public static void spare(MemorySegment struct, long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

}

