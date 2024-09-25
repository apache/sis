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

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt latencyLAYOUT = (OfInt)LAYOUT.select(groupElement("latency"));

    private static final long latency$OFFSET = 0;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int latency
     * }
     */
    public int latency() {
        return struct.get(latencyLAYOUT, latency$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int latency
     * }
     */
    public void latency(int fieldValue) {
        struct.set(latencyLAYOUT, latency$OFFSET, fieldValue);
    }

    private static final OfInt ping_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_number"));

    private static final long ping_number$OFFSET = 4;

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

    private static final OfInt sonar_idLAYOUT = (OfInt)LAYOUT.select(groupElement("sonar_id"));

    private static final long sonar_id$OFFSET = 8;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int sonar_id
     * }
     */
    public int sonar_id() {
        return struct.get(sonar_idLAYOUT, sonar_id$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int sonar_id
     * }
     */
    public void sonar_id(int fieldValue) {
        struct.set(sonar_idLAYOUT, sonar_id$OFFSET, fieldValue);
    }

    private static final OfInt sonar_modelLAYOUT = (OfInt)LAYOUT.select(groupElement("sonar_model"));

    private static final long sonar_model$OFFSET = 12;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int sonar_model
     * }
     */
    public int sonar_model() {
        return struct.get(sonar_modelLAYOUT, sonar_model$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int sonar_model
     * }
     */
    public void sonar_model(int fieldValue) {
        struct.set(sonar_modelLAYOUT, sonar_model$OFFSET, fieldValue);
    }

    private static final OfInt frequencyLAYOUT = (OfInt)LAYOUT.select(groupElement("frequency"));

    private static final long frequency$OFFSET = 16;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int frequency
     * }
     */
    public int frequency() {
        return struct.get(frequencyLAYOUT, frequency$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int frequency
     * }
     */
    public void frequency(int fieldValue) {
        struct.set(frequencyLAYOUT, frequency$OFFSET, fieldValue);
    }

    private static final OfDouble surface_velocityLAYOUT = (OfDouble)LAYOUT.select(groupElement("surface_velocity"));

    private static final long surface_velocity$OFFSET = 24;

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

    private static final OfInt sample_rateLAYOUT = (OfInt)LAYOUT.select(groupElement("sample_rate"));

    private static final long sample_rate$OFFSET = 32;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int sample_rate
     * }
     */
    public int sample_rate() {
        return struct.get(sample_rateLAYOUT, sample_rate$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int sample_rate
     * }
     */
    public void sample_rate(int fieldValue) {
        struct.set(sample_rateLAYOUT, sample_rate$OFFSET, fieldValue);
    }

    private static final OfInt ping_rateLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_rate"));

    private static final long ping_rate$OFFSET = 36;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_rate
     * }
     */
    public int ping_rate() {
        return struct.get(ping_rateLAYOUT, ping_rate$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_rate
     * }
     */
    public void ping_rate(int fieldValue) {
        struct.set(ping_rateLAYOUT, ping_rate$OFFSET, fieldValue);
    }

    private static final OfInt modeLAYOUT = (OfInt)LAYOUT.select(groupElement("mode"));

    private static final long mode$OFFSET = 40;

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

    private static final long range$OFFSET = 44;

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

    private static final long power$OFFSET = 48;

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

    private static final long gain$OFFSET = 52;

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

    private static final long pulse_width$OFFSET = 56;

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

    private static final long tvg_spreading$OFFSET = 60;

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

    private static final long tvg_absorption$OFFSET = 64;

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

    private static final long fore_aft_bw$OFFSET = 72;

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

    private static final long athwart_bw$OFFSET = 80;

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

    private static final OfInt projector_typeLAYOUT = (OfInt)LAYOUT.select(groupElement("projector_type"));

    private static final long projector_type$OFFSET = 88;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int projector_type
     * }
     */
    public int projector_type() {
        return struct.get(projector_typeLAYOUT, projector_type$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int projector_type
     * }
     */
    public void projector_type(int fieldValue) {
        struct.set(projector_typeLAYOUT, projector_type$OFFSET, fieldValue);
    }

    private static final OfInt projector_angleLAYOUT = (OfInt)LAYOUT.select(groupElement("projector_angle"));

    private static final long projector_angle$OFFSET = 92;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int projector_angle
     * }
     */
    public int projector_angle() {
        return struct.get(projector_angleLAYOUT, projector_angle$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int projector_angle
     * }
     */
    public void projector_angle(int fieldValue) {
        struct.set(projector_angleLAYOUT, projector_angle$OFFSET, fieldValue);
    }

    private static final OfDouble range_filt_minLAYOUT = (OfDouble)LAYOUT.select(groupElement("range_filt_min"));

    private static final long range_filt_min$OFFSET = 96;

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

    private static final long range_filt_max$OFFSET = 104;

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

    private static final long depth_filt_min$OFFSET = 112;

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

    private static final long depth_filt_max$OFFSET = 120;

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

    private static final OfInt filters_activeLAYOUT = (OfInt)LAYOUT.select(groupElement("filters_active"));

    private static final long filters_active$OFFSET = 128;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int filters_active
     * }
     */
    public int filters_active() {
        return struct.get(filters_activeLAYOUT, filters_active$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int filters_active
     * }
     */
    public void filters_active(int fieldValue) {
        struct.set(filters_activeLAYOUT, filters_active$OFFSET, fieldValue);
    }

    private static final OfInt temperatureLAYOUT = (OfInt)LAYOUT.select(groupElement("temperature"));

    private static final long temperature$OFFSET = 132;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int temperature
     * }
     */
    public int temperature() {
        return struct.get(temperatureLAYOUT, temperature$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int temperature
     * }
     */
    public void temperature(int fieldValue) {
        struct.set(temperatureLAYOUT, temperature$OFFSET, fieldValue);
    }

    private static final OfDouble beam_spacingLAYOUT = (OfDouble)LAYOUT.select(groupElement("beam_spacing"));

    private static final long beam_spacing$OFFSET = 136;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double beam_spacing
     * }
     */
    public double beam_spacing() {
        return struct.get(beam_spacingLAYOUT, beam_spacing$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double beam_spacing
     * }
     */
    public void beam_spacing(double fieldValue) {
        struct.set(beam_spacingLAYOUT, beam_spacing$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    private static final long spare$OFFSET = 144;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public MemorySegment spare() {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public void spare(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public byte spare(long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public void spare(long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }
}
