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
public final class Reson8100Specific extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("latency"),
        GSF.C_INT.withName("ping_number"),
        GSF.C_INT.withName("sonar_id"),
        GSF.C_INT.withName("sonar_model"),
        GSF.C_INT.withName("frequency"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("surface_velocity"),
        GSF.C_INT.withName("sample_rate"),
        GSF.C_INT.withName("ping_rate"),
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
        GSF.C_INT.withName("projector_type"),
        GSF.C_INT.withName("projector_angle"),
        GSF.C_DOUBLE.withName("range_filt_min"),
        GSF.C_DOUBLE.withName("range_filt_max"),
        GSF.C_DOUBLE.withName("depth_filt_min"),
        GSF.C_DOUBLE.withName("depth_filt_max"),
        GSF.C_INT.withName("filters_active"),
        GSF.C_INT.withName("temperature"),
        GSF.C_DOUBLE.withName("beam_spacing"),
        MemoryLayout.sequenceLayout(2, GSF.C_CHAR).withName("spare"),
        MemoryLayout.paddingLayout(6)
    ).withName("t_gsfReson8100Specific");

    public Reson8100Specific(MemorySegment struct) {
        super(struct);
    }

    public Reson8100Specific(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt latencyLAYOUT = (OfInt)LAYOUT.select(groupElement("latency"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int latency
     * }
     */
    public static final OfInt latencyLAYOUT() {
        return latencyLAYOUT;
    }

    private static final long latency$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int latency
     * }
     */
    public static final long latency$offset() {
        return latency$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int latency
     * }
     */
    public static int latency(MemorySegment struct) {
        return struct.get(latencyLAYOUT, latency$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int latency
     * }
     */
    public static void latency(MemorySegment struct, int fieldValue) {
        struct.set(latencyLAYOUT, latency$OFFSET, fieldValue);
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

    private static final long ping_number$OFFSET = 4;

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

    private static final OfInt sonar_idLAYOUT = (OfInt)LAYOUT.select(groupElement("sonar_id"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int sonar_id
     * }
     */
    public static final OfInt sonar_idLAYOUT() {
        return sonar_idLAYOUT;
    }

    private static final long sonar_id$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int sonar_id
     * }
     */
    public static final long sonar_id$offset() {
        return sonar_id$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int sonar_id
     * }
     */
    public static int sonar_id(MemorySegment struct) {
        return struct.get(sonar_idLAYOUT, sonar_id$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int sonar_id
     * }
     */
    public static void sonar_id(MemorySegment struct, int fieldValue) {
        struct.set(sonar_idLAYOUT, sonar_id$OFFSET, fieldValue);
    }

    private static final OfInt sonar_modelLAYOUT = (OfInt)LAYOUT.select(groupElement("sonar_model"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int sonar_model
     * }
     */
    public static final OfInt sonar_modelLAYOUT() {
        return sonar_modelLAYOUT;
    }

    private static final long sonar_model$OFFSET = 12;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int sonar_model
     * }
     */
    public static final long sonar_model$offset() {
        return sonar_model$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int sonar_model
     * }
     */
    public static int sonar_model(MemorySegment struct) {
        return struct.get(sonar_modelLAYOUT, sonar_model$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int sonar_model
     * }
     */
    public static void sonar_model(MemorySegment struct, int fieldValue) {
        struct.set(sonar_modelLAYOUT, sonar_model$OFFSET, fieldValue);
    }

    private static final OfInt frequencyLAYOUT = (OfInt)LAYOUT.select(groupElement("frequency"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int frequency
     * }
     */
    public static final OfInt frequencyLAYOUT() {
        return frequencyLAYOUT;
    }

    private static final long frequency$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int frequency
     * }
     */
    public static final long frequency$offset() {
        return frequency$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int frequency
     * }
     */
    public static int frequency(MemorySegment struct) {
        return struct.get(frequencyLAYOUT, frequency$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int frequency
     * }
     */
    public static void frequency(MemorySegment struct, int fieldValue) {
        struct.set(frequencyLAYOUT, frequency$OFFSET, fieldValue);
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

    private static final long surface_velocity$OFFSET = 24;

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

    private static final OfInt sample_rateLAYOUT = (OfInt)LAYOUT.select(groupElement("sample_rate"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int sample_rate
     * }
     */
    public static final OfInt sample_rateLAYOUT() {
        return sample_rateLAYOUT;
    }

    private static final long sample_rate$OFFSET = 32;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int sample_rate
     * }
     */
    public static final long sample_rate$offset() {
        return sample_rate$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int sample_rate
     * }
     */
    public static int sample_rate(MemorySegment struct) {
        return struct.get(sample_rateLAYOUT, sample_rate$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int sample_rate
     * }
     */
    public static void sample_rate(MemorySegment struct, int fieldValue) {
        struct.set(sample_rateLAYOUT, sample_rate$OFFSET, fieldValue);
    }

    private static final OfInt ping_rateLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_rate"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int ping_rate
     * }
     */
    public static final OfInt ping_rateLAYOUT() {
        return ping_rateLAYOUT;
    }

    private static final long ping_rate$OFFSET = 36;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int ping_rate
     * }
     */
    public static final long ping_rate$offset() {
        return ping_rate$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_rate
     * }
     */
    public static int ping_rate(MemorySegment struct) {
        return struct.get(ping_rateLAYOUT, ping_rate$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_rate
     * }
     */
    public static void ping_rate(MemorySegment struct, int fieldValue) {
        struct.set(ping_rateLAYOUT, ping_rate$OFFSET, fieldValue);
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

    private static final long mode$OFFSET = 40;

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

    private static final long range$OFFSET = 44;

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

    private static final long power$OFFSET = 48;

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

    private static final long gain$OFFSET = 52;

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

    private static final long pulse_width$OFFSET = 56;

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

    private static final long tvg_spreading$OFFSET = 60;

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

    private static final long tvg_absorption$OFFSET = 64;

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

    private static final long fore_aft_bw$OFFSET = 72;

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

    private static final long athwart_bw$OFFSET = 80;

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

    private static final OfInt projector_typeLAYOUT = (OfInt)LAYOUT.select(groupElement("projector_type"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int projector_type
     * }
     */
    public static final OfInt projector_typeLAYOUT() {
        return projector_typeLAYOUT;
    }

    private static final long projector_type$OFFSET = 88;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int projector_type
     * }
     */
    public static final long projector_type$offset() {
        return projector_type$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int projector_type
     * }
     */
    public static int projector_type(MemorySegment struct) {
        return struct.get(projector_typeLAYOUT, projector_type$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int projector_type
     * }
     */
    public static void projector_type(MemorySegment struct, int fieldValue) {
        struct.set(projector_typeLAYOUT, projector_type$OFFSET, fieldValue);
    }

    private static final OfInt projector_angleLAYOUT = (OfInt)LAYOUT.select(groupElement("projector_angle"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int projector_angle
     * }
     */
    public static final OfInt projector_angleLAYOUT() {
        return projector_angleLAYOUT;
    }

    private static final long projector_angle$OFFSET = 92;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int projector_angle
     * }
     */
    public static final long projector_angle$offset() {
        return projector_angle$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int projector_angle
     * }
     */
    public static int projector_angle(MemorySegment struct) {
        return struct.get(projector_angleLAYOUT, projector_angle$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int projector_angle
     * }
     */
    public static void projector_angle(MemorySegment struct, int fieldValue) {
        struct.set(projector_angleLAYOUT, projector_angle$OFFSET, fieldValue);
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

    private static final long range_filt_min$OFFSET = 96;

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

    private static final long range_filt_max$OFFSET = 104;

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

    private static final long depth_filt_min$OFFSET = 112;

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

    private static final long depth_filt_max$OFFSET = 120;

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

    private static final OfInt filters_activeLAYOUT = (OfInt)LAYOUT.select(groupElement("filters_active"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int filters_active
     * }
     */
    public static final OfInt filters_activeLAYOUT() {
        return filters_activeLAYOUT;
    }

    private static final long filters_active$OFFSET = 128;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int filters_active
     * }
     */
    public static final long filters_active$offset() {
        return filters_active$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int filters_active
     * }
     */
    public static int filters_active(MemorySegment struct) {
        return struct.get(filters_activeLAYOUT, filters_active$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int filters_active
     * }
     */
    public static void filters_active(MemorySegment struct, int fieldValue) {
        struct.set(filters_activeLAYOUT, filters_active$OFFSET, fieldValue);
    }

    private static final OfInt temperatureLAYOUT = (OfInt)LAYOUT.select(groupElement("temperature"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int temperature
     * }
     */
    public static final OfInt temperatureLAYOUT() {
        return temperatureLAYOUT;
    }

    private static final long temperature$OFFSET = 132;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int temperature
     * }
     */
    public static final long temperature$offset() {
        return temperature$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int temperature
     * }
     */
    public static int temperature(MemorySegment struct) {
        return struct.get(temperatureLAYOUT, temperature$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int temperature
     * }
     */
    public static void temperature(MemorySegment struct, int fieldValue) {
        struct.set(temperatureLAYOUT, temperature$OFFSET, fieldValue);
    }

    private static final OfDouble beam_spacingLAYOUT = (OfDouble)LAYOUT.select(groupElement("beam_spacing"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double beam_spacing
     * }
     */
    public static final OfDouble beam_spacingLAYOUT() {
        return beam_spacingLAYOUT;
    }

    private static final long beam_spacing$OFFSET = 136;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double beam_spacing
     * }
     */
    public static final long beam_spacing$offset() {
        return beam_spacing$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double beam_spacing
     * }
     */
    public static double beam_spacing(MemorySegment struct) {
        return struct.get(beam_spacingLAYOUT, beam_spacing$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double beam_spacing
     * }
     */
    public static void beam_spacing(MemorySegment struct, double fieldValue) {
        struct.set(beam_spacingLAYOUT, beam_spacing$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public static final SequenceLayout spareLAYOUT() {
        return spareLAYOUT;
    }

    private static final long spare$OFFSET = 144;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public static final long spare$offset() {
        return spare$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public static MemorySegment spare(MemorySegment struct) {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public static void spare(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static long[] spare$DIMS = { 2 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public static long[] spare$dimensions() {
        return spare$DIMS;
    }
    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public static byte spare(MemorySegment struct, long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public static void spare(MemorySegment struct, long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

}

