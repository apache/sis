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

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt ping_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_number"));

    private static final long ping_number$OFFSET = 0;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_number
     * }
     */
    public int ping_number() {
        return struct.get(ping_numberLAYOUT, ping_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_number
     * }
     */
    public void ping_number(int fieldValue) {
        struct.set(ping_numberLAYOUT, ping_number$OFFSET, fieldValue);
    }

    private static final OfDouble surface_velocityLAYOUT = (OfDouble)LAYOUT.select(groupElement("surface_velocity"));

    private static final long surface_velocity$OFFSET = 8;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public double surface_velocity() {
        return struct.get(surface_velocityLAYOUT, surface_velocity$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public void surface_velocity(double fieldValue) {
        struct.set(surface_velocityLAYOUT, surface_velocity$OFFSET, fieldValue);
    }

    private static final OfInt modeLAYOUT = (OfInt)LAYOUT.select(groupElement("mode"));

    private static final long mode$OFFSET = 16;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int mode
     * }
     */
    public int mode() {
        return struct.get(modeLAYOUT, mode$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int mode
     * }
     */
    public void mode(int fieldValue) {
        struct.set(modeLAYOUT, mode$OFFSET, fieldValue);
    }

    private static final OfInt rangeLAYOUT = (OfInt)LAYOUT.select(groupElement("range"));

    private static final long range$OFFSET = 20;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int range
     * }
     */
    public int range() {
        return struct.get(rangeLAYOUT, range$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int range
     * }
     */
    public void range(int fieldValue) {
        struct.set(rangeLAYOUT, range$OFFSET, fieldValue);
    }

    private static final OfInt powerLAYOUT = (OfInt)LAYOUT.select(groupElement("power"));

    private static final long power$OFFSET = 24;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int power
     * }
     */
    public int power() {
        return struct.get(powerLAYOUT, power$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int power
     * }
     */
    public void power(int fieldValue) {
        struct.set(powerLAYOUT, power$OFFSET, fieldValue);
    }

    private static final OfInt gainLAYOUT = (OfInt)LAYOUT.select(groupElement("gain"));

    private static final long gain$OFFSET = 28;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int gain
     * }
     */
    public int gain() {
        return struct.get(gainLAYOUT, gain$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int gain
     * }
     */
    public void gain(int fieldValue) {
        struct.set(gainLAYOUT, gain$OFFSET, fieldValue);
    }

    private static final OfInt pulse_widthLAYOUT = (OfInt)LAYOUT.select(groupElement("pulse_width"));

    private static final long pulse_width$OFFSET = 32;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int pulse_width
     * }
     */
    public int pulse_width() {
        return struct.get(pulse_widthLAYOUT, pulse_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int pulse_width
     * }
     */
    public void pulse_width(int fieldValue) {
        struct.set(pulse_widthLAYOUT, pulse_width$OFFSET, fieldValue);
    }

    private static final OfInt tvg_spreadingLAYOUT = (OfInt)LAYOUT.select(groupElement("tvg_spreading"));

    private static final long tvg_spreading$OFFSET = 36;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int tvg_spreading
     * }
     */
    public int tvg_spreading() {
        return struct.get(tvg_spreadingLAYOUT, tvg_spreading$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int tvg_spreading
     * }
     */
    public void tvg_spreading(int fieldValue) {
        struct.set(tvg_spreadingLAYOUT, tvg_spreading$OFFSET, fieldValue);
    }

    private static final OfInt tvg_absorptionLAYOUT = (OfInt)LAYOUT.select(groupElement("tvg_absorption"));

    private static final long tvg_absorption$OFFSET = 40;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int tvg_absorption
     * }
     */
    public int tvg_absorption() {
        return struct.get(tvg_absorptionLAYOUT, tvg_absorption$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int tvg_absorption
     * }
     */
    public void tvg_absorption(int fieldValue) {
        struct.set(tvg_absorptionLAYOUT, tvg_absorption$OFFSET, fieldValue);
    }

    private static final OfDouble fore_aft_bwLAYOUT = (OfDouble)LAYOUT.select(groupElement("fore_aft_bw"));

    private static final long fore_aft_bw$OFFSET = 48;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double fore_aft_bw
     * }
     */
    public double fore_aft_bw() {
        return struct.get(fore_aft_bwLAYOUT, fore_aft_bw$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double fore_aft_bw
     * }
     */
    public void fore_aft_bw(double fieldValue) {
        struct.set(fore_aft_bwLAYOUT, fore_aft_bw$OFFSET, fieldValue);
    }

    private static final OfDouble athwart_bwLAYOUT = (OfDouble)LAYOUT.select(groupElement("athwart_bw"));

    private static final long athwart_bw$OFFSET = 56;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double athwart_bw
     * }
     */
    public double athwart_bw() {
        return struct.get(athwart_bwLAYOUT, athwart_bw$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double athwart_bw
     * }
     */
    public void athwart_bw(double fieldValue) {
        struct.set(athwart_bwLAYOUT, athwart_bw$OFFSET, fieldValue);
    }

    private static final OfDouble range_filt_minLAYOUT = (OfDouble)LAYOUT.select(groupElement("range_filt_min"));

    private static final long range_filt_min$OFFSET = 64;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double range_filt_min
     * }
     */
    public double range_filt_min() {
        return struct.get(range_filt_minLAYOUT, range_filt_min$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double range_filt_min
     * }
     */
    public void range_filt_min(double fieldValue) {
        struct.set(range_filt_minLAYOUT, range_filt_min$OFFSET, fieldValue);
    }

    private static final OfDouble range_filt_maxLAYOUT = (OfDouble)LAYOUT.select(groupElement("range_filt_max"));

    private static final long range_filt_max$OFFSET = 72;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double range_filt_max
     * }
     */
    public double range_filt_max() {
        return struct.get(range_filt_maxLAYOUT, range_filt_max$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double range_filt_max
     * }
     */
    public void range_filt_max(double fieldValue) {
        struct.set(range_filt_maxLAYOUT, range_filt_max$OFFSET, fieldValue);
    }

    private static final OfDouble depth_filt_minLAYOUT = (OfDouble)LAYOUT.select(groupElement("depth_filt_min"));

    private static final long depth_filt_min$OFFSET = 80;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double depth_filt_min
     * }
     */
    public double depth_filt_min() {
        return struct.get(depth_filt_minLAYOUT, depth_filt_min$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double depth_filt_min
     * }
     */
    public void depth_filt_min(double fieldValue) {
        struct.set(depth_filt_minLAYOUT, depth_filt_min$OFFSET, fieldValue);
    }

    private static final OfDouble depth_filt_maxLAYOUT = (OfDouble)LAYOUT.select(groupElement("depth_filt_max"));

    private static final long depth_filt_max$OFFSET = 88;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double depth_filt_max
     * }
     */
    public double depth_filt_max() {
        return struct.get(depth_filt_maxLAYOUT, depth_filt_max$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double depth_filt_max
     * }
     */
    public void depth_filt_max(double fieldValue) {
        struct.set(depth_filt_maxLAYOUT, depth_filt_max$OFFSET, fieldValue);
    }

    private static final OfInt projectorLAYOUT = (OfInt)LAYOUT.select(groupElement("projector"));

    private static final long projector$OFFSET = 96;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int projector
     * }
     */
    public int projector() {
        return struct.get(projectorLAYOUT, projector$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int projector
     * }
     */
    public void projector(int fieldValue) {
        struct.set(projectorLAYOUT, projector$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    private static final long spare$OFFSET = 100;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public MemorySegment spare() {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public void spare(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public byte spare(long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public void spare(long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }
}
