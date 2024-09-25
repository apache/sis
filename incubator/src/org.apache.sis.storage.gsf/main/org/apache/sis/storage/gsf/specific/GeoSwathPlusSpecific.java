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
public final class GeoSwathPlusSpecific extends StructClass {
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("data_source"),
        GSF.C_INT.withName("side"),
        GSF.C_INT.withName("model_number"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("frequency"),
        GSF.C_INT.withName("echosounder_type"),
        MemoryLayout.paddingLayout(4),
        GSF.C_LONG.withName("ping_number"),
        GSF.C_INT.withName("num_nav_samples"),
        GSF.C_INT.withName("num_attitude_samples"),
        GSF.C_INT.withName("num_heading_samples"),
        GSF.C_INT.withName("num_miniSVS_samples"),
        GSF.C_INT.withName("num_echosounder_samples"),
        GSF.C_INT.withName("num_raa_samples"),
        GSF.C_DOUBLE.withName("mean_sv"),
        GSF.C_DOUBLE.withName("surface_velocity"),
        GSF.C_INT.withName("valid_beams"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("sample_rate"),
        GSF.C_DOUBLE.withName("pulse_length"),
        GSF.C_INT.withName("ping_length"),
        GSF.C_INT.withName("transmit_power"),
        GSF.C_INT.withName("sidescan_gain_channel"),
        GSF.C_INT.withName("stabilization"),
        GSF.C_INT.withName("gps_quality"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("range_uncertainty"),
        GSF.C_DOUBLE.withName("angle_uncertainty"),
        MemoryLayout.sequenceLayout(32, GSF.C_CHAR).withName("spare")
    ).withName("t_gsfGeoSwathPlusSpecific");

    public GeoSwathPlusSpecific(MemorySegment struct) {
        super(struct);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt data_sourceLAYOUT = (OfInt)LAYOUT.select(groupElement("data_source"));

    private static final long data_source$OFFSET = 0;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int data_source
     * }
     */
    public int data_source() {
        return struct.get(data_sourceLAYOUT, data_source$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int data_source
     * }
     */
    public void data_source(int fieldValue) {
        struct.set(data_sourceLAYOUT, data_source$OFFSET, fieldValue);
    }

    private static final OfInt sideLAYOUT = (OfInt)LAYOUT.select(groupElement("side"));

    private static final long side$OFFSET = 4;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int side
     * }
     */
    public int side() {
        return struct.get(sideLAYOUT, side$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int side
     * }
     */
    public void side(int fieldValue) {
        struct.set(sideLAYOUT, side$OFFSET, fieldValue);
    }

    private static final OfInt model_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("model_number"));

    private static final long model_number$OFFSET = 8;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int model_number
     * }
     */
    public int model_number() {
        return struct.get(model_numberLAYOUT, model_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int model_number
     * }
     */
    public void model_number(int fieldValue) {
        struct.set(model_numberLAYOUT, model_number$OFFSET, fieldValue);
    }

    private static final OfDouble frequencyLAYOUT = (OfDouble)LAYOUT.select(groupElement("frequency"));

    private static final long frequency$OFFSET = 16;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double frequency
     * }
     */
    public double frequency() {
        return struct.get(frequencyLAYOUT, frequency$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double frequency
     * }
     */
    public void frequency(double fieldValue) {
        struct.set(frequencyLAYOUT, frequency$OFFSET, fieldValue);
    }

    private static final OfInt echosounder_typeLAYOUT = (OfInt)LAYOUT.select(groupElement("echosounder_type"));

    private static final long echosounder_type$OFFSET = 24;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int echosounder_type
     * }
     */
    public int echosounder_type() {
        return struct.get(echosounder_typeLAYOUT, echosounder_type$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int echosounder_type
     * }
     */
    public void echosounder_type(int fieldValue) {
        struct.set(echosounder_typeLAYOUT, echosounder_type$OFFSET, fieldValue);
    }

    private static final OfLong ping_numberLAYOUT = (OfLong)LAYOUT.select(groupElement("ping_number"));

    private static final long ping_number$OFFSET = 32;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * long ping_number
     * }
     */
    public long ping_number() {
        return struct.get(ping_numberLAYOUT, ping_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * long ping_number
     * }
     */
    public void ping_number(long fieldValue) {
        struct.set(ping_numberLAYOUT, ping_number$OFFSET, fieldValue);
    }

    private static final OfInt num_nav_samplesLAYOUT = (OfInt)LAYOUT.select(groupElement("num_nav_samples"));

    private static final long num_nav_samples$OFFSET = 40;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int num_nav_samples
     * }
     */
    public int num_nav_samples() {
        return struct.get(num_nav_samplesLAYOUT, num_nav_samples$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int num_nav_samples
     * }
     */
    public void num_nav_samples(int fieldValue) {
        struct.set(num_nav_samplesLAYOUT, num_nav_samples$OFFSET, fieldValue);
    }

    private static final OfInt num_attitude_samplesLAYOUT = (OfInt)LAYOUT.select(groupElement("num_attitude_samples"));

    private static final long num_attitude_samples$OFFSET = 44;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int num_attitude_samples
     * }
     */
    public int num_attitude_samples() {
        return struct.get(num_attitude_samplesLAYOUT, num_attitude_samples$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int num_attitude_samples
     * }
     */
    public void num_attitude_samples(int fieldValue) {
        struct.set(num_attitude_samplesLAYOUT, num_attitude_samples$OFFSET, fieldValue);
    }

    private static final OfInt num_heading_samplesLAYOUT = (OfInt)LAYOUT.select(groupElement("num_heading_samples"));

    private static final long num_heading_samples$OFFSET = 48;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int num_heading_samples
     * }
     */
    public int num_heading_samples() {
        return struct.get(num_heading_samplesLAYOUT, num_heading_samples$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int num_heading_samples
     * }
     */
    public void num_heading_samples(int fieldValue) {
        struct.set(num_heading_samplesLAYOUT, num_heading_samples$OFFSET, fieldValue);
    }

    private static final OfInt num_miniSVS_samplesLAYOUT = (OfInt)LAYOUT.select(groupElement("num_miniSVS_samples"));

    private static final long num_miniSVS_samples$OFFSET = 52;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int num_miniSVS_samples
     * }
     */
    public int num_miniSVS_samples() {
        return struct.get(num_miniSVS_samplesLAYOUT, num_miniSVS_samples$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int num_miniSVS_samples
     * }
     */
    public void num_miniSVS_samples(int fieldValue) {
        struct.set(num_miniSVS_samplesLAYOUT, num_miniSVS_samples$OFFSET, fieldValue);
    }

    private static final OfInt num_echosounder_samplesLAYOUT = (OfInt)LAYOUT.select(groupElement("num_echosounder_samples"));

    private static final long num_echosounder_samples$OFFSET = 56;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int num_echosounder_samples
     * }
     */
    public int num_echosounder_samples() {
        return struct.get(num_echosounder_samplesLAYOUT, num_echosounder_samples$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int num_echosounder_samples
     * }
     */
    public void num_echosounder_samples(int fieldValue) {
        struct.set(num_echosounder_samplesLAYOUT, num_echosounder_samples$OFFSET, fieldValue);
    }

    private static final OfInt num_raa_samplesLAYOUT = (OfInt)LAYOUT.select(groupElement("num_raa_samples"));

    private static final long num_raa_samples$OFFSET = 60;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int num_raa_samples
     * }
     */
    public int num_raa_samples() {
        return struct.get(num_raa_samplesLAYOUT, num_raa_samples$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int num_raa_samples
     * }
     */
    public void num_raa_samples(int fieldValue) {
        struct.set(num_raa_samplesLAYOUT, num_raa_samples$OFFSET, fieldValue);
    }

    private static final OfDouble mean_svLAYOUT = (OfDouble)LAYOUT.select(groupElement("mean_sv"));

    private static final long mean_sv$OFFSET = 64;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double mean_sv
     * }
     */
    public double mean_sv() {
        return struct.get(mean_svLAYOUT, mean_sv$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double mean_sv
     * }
     */
    public void mean_sv(double fieldValue) {
        struct.set(mean_svLAYOUT, mean_sv$OFFSET, fieldValue);
    }

    private static final OfDouble surface_velocityLAYOUT = (OfDouble)LAYOUT.select(groupElement("surface_velocity"));

    private static final long surface_velocity$OFFSET = 72;

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

    private static final OfInt valid_beamsLAYOUT = (OfInt)LAYOUT.select(groupElement("valid_beams"));

    private static final long valid_beams$OFFSET = 80;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int valid_beams
     * }
     */
    public int valid_beams() {
        return struct.get(valid_beamsLAYOUT, valid_beams$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int valid_beams
     * }
     */
    public void valid_beams(int fieldValue) {
        struct.set(valid_beamsLAYOUT, valid_beams$OFFSET, fieldValue);
    }

    private static final OfDouble sample_rateLAYOUT = (OfDouble)LAYOUT.select(groupElement("sample_rate"));

    private static final long sample_rate$OFFSET = 88;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double sample_rate
     * }
     */
    public double sample_rate() {
        return struct.get(sample_rateLAYOUT, sample_rate$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double sample_rate
     * }
     */
    public void sample_rate(double fieldValue) {
        struct.set(sample_rateLAYOUT, sample_rate$OFFSET, fieldValue);
    }

    private static final OfDouble pulse_lengthLAYOUT = (OfDouble)LAYOUT.select(groupElement("pulse_length"));

    private static final long pulse_length$OFFSET = 96;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double pulse_length
     * }
     */
    public double pulse_length() {
        return struct.get(pulse_lengthLAYOUT, pulse_length$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double pulse_length
     * }
     */
    public void pulse_length(double fieldValue) {
        struct.set(pulse_lengthLAYOUT, pulse_length$OFFSET, fieldValue);
    }

    private static final OfInt ping_lengthLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_length"));

    private static final long ping_length$OFFSET = 104;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_length
     * }
     */
    public int ping_length() {
        return struct.get(ping_lengthLAYOUT, ping_length$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_length
     * }
     */
    public void ping_length(int fieldValue) {
        struct.set(ping_lengthLAYOUT, ping_length$OFFSET, fieldValue);
    }

    private static final OfInt transmit_powerLAYOUT = (OfInt)LAYOUT.select(groupElement("transmit_power"));

    private static final long transmit_power$OFFSET = 108;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int transmit_power
     * }
     */
    public int transmit_power() {
        return struct.get(transmit_powerLAYOUT, transmit_power$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int transmit_power
     * }
     */
    public void transmit_power(int fieldValue) {
        struct.set(transmit_powerLAYOUT, transmit_power$OFFSET, fieldValue);
    }

    private static final OfInt sidescan_gain_channelLAYOUT = (OfInt)LAYOUT.select(groupElement("sidescan_gain_channel"));

    private static final long sidescan_gain_channel$OFFSET = 112;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int sidescan_gain_channel
     * }
     */
    public int sidescan_gain_channel() {
        return struct.get(sidescan_gain_channelLAYOUT, sidescan_gain_channel$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int sidescan_gain_channel
     * }
     */
    public void sidescan_gain_channel(int fieldValue) {
        struct.set(sidescan_gain_channelLAYOUT, sidescan_gain_channel$OFFSET, fieldValue);
    }

    private static final OfInt stabilizationLAYOUT = (OfInt)LAYOUT.select(groupElement("stabilization"));

    private static final long stabilization$OFFSET = 116;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int stabilization
     * }
     */
    public int stabilization() {
        return struct.get(stabilizationLAYOUT, stabilization$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int stabilization
     * }
     */
    public void stabilization(int fieldValue) {
        struct.set(stabilizationLAYOUT, stabilization$OFFSET, fieldValue);
    }

    private static final OfInt gps_qualityLAYOUT = (OfInt)LAYOUT.select(groupElement("gps_quality"));

    private static final long gps_quality$OFFSET = 120;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int gps_quality
     * }
     */
    public int gps_quality() {
        return struct.get(gps_qualityLAYOUT, gps_quality$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int gps_quality
     * }
     */
    public void gps_quality(int fieldValue) {
        struct.set(gps_qualityLAYOUT, gps_quality$OFFSET, fieldValue);
    }

    private static final OfDouble range_uncertaintyLAYOUT = (OfDouble)LAYOUT.select(groupElement("range_uncertainty"));

    private static final long range_uncertainty$OFFSET = 128;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double range_uncertainty
     * }
     */
    public double range_uncertainty() {
        return struct.get(range_uncertaintyLAYOUT, range_uncertainty$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double range_uncertainty
     * }
     */
    public void range_uncertainty(double fieldValue) {
        struct.set(range_uncertaintyLAYOUT, range_uncertainty$OFFSET, fieldValue);
    }

    private static final OfDouble angle_uncertaintyLAYOUT = (OfDouble)LAYOUT.select(groupElement("angle_uncertainty"));

    private static final long angle_uncertainty$OFFSET = 136;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double angle_uncertainty
     * }
     */
    public double angle_uncertainty() {
        return struct.get(angle_uncertaintyLAYOUT, angle_uncertainty$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double angle_uncertainty
     * }
     */
    public void angle_uncertainty(double fieldValue) {
        struct.set(angle_uncertaintyLAYOUT, angle_uncertainty$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    private static final long spare$OFFSET = 144;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char spare[32]
     * }
     */
    public MemorySegment spare() {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char spare[32]
     * }
     */
    public void spare(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * char spare[32]
     * }
     */
    public byte spare(long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * char spare[32]
     * }
     */
    public void spare(long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }
}
