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

    public GeoSwathPlusSpecific(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt data_sourceLAYOUT = (OfInt)LAYOUT.select(groupElement("data_source"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int data_source
     * }
     */
    public static final OfInt data_sourceLAYOUT() {
        return data_sourceLAYOUT;
    }

    private static final long data_source$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int data_source
     * }
     */
    public static final long data_source$offset() {
        return data_source$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int data_source
     * }
     */
    public static int data_source(MemorySegment struct) {
        return struct.get(data_sourceLAYOUT, data_source$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int data_source
     * }
     */
    public static void data_source(MemorySegment struct, int fieldValue) {
        struct.set(data_sourceLAYOUT, data_source$OFFSET, fieldValue);
    }

    private static final OfInt sideLAYOUT = (OfInt)LAYOUT.select(groupElement("side"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int side
     * }
     */
    public static final OfInt sideLAYOUT() {
        return sideLAYOUT;
    }

    private static final long side$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int side
     * }
     */
    public static final long side$offset() {
        return side$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int side
     * }
     */
    public static int side(MemorySegment struct) {
        return struct.get(sideLAYOUT, side$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int side
     * }
     */
    public static void side(MemorySegment struct, int fieldValue) {
        struct.set(sideLAYOUT, side$OFFSET, fieldValue);
    }

    private static final OfInt model_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("model_number"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int model_number
     * }
     */
    public static final OfInt model_numberLAYOUT() {
        return model_numberLAYOUT;
    }

    private static final long model_number$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int model_number
     * }
     */
    public static final long model_number$offset() {
        return model_number$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int model_number
     * }
     */
    public static int model_number(MemorySegment struct) {
        return struct.get(model_numberLAYOUT, model_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int model_number
     * }
     */
    public static void model_number(MemorySegment struct, int fieldValue) {
        struct.set(model_numberLAYOUT, model_number$OFFSET, fieldValue);
    }

    private static final OfDouble frequencyLAYOUT = (OfDouble)LAYOUT.select(groupElement("frequency"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double frequency
     * }
     */
    public static final OfDouble frequencyLAYOUT() {
        return frequencyLAYOUT;
    }

    private static final long frequency$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double frequency
     * }
     */
    public static final long frequency$offset() {
        return frequency$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double frequency
     * }
     */
    public static double frequency(MemorySegment struct) {
        return struct.get(frequencyLAYOUT, frequency$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double frequency
     * }
     */
    public static void frequency(MemorySegment struct, double fieldValue) {
        struct.set(frequencyLAYOUT, frequency$OFFSET, fieldValue);
    }

    private static final OfInt echosounder_typeLAYOUT = (OfInt)LAYOUT.select(groupElement("echosounder_type"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int echosounder_type
     * }
     */
    public static final OfInt echosounder_typeLAYOUT() {
        return echosounder_typeLAYOUT;
    }

    private static final long echosounder_type$OFFSET = 24;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int echosounder_type
     * }
     */
    public static final long echosounder_type$offset() {
        return echosounder_type$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int echosounder_type
     * }
     */
    public static int echosounder_type(MemorySegment struct) {
        return struct.get(echosounder_typeLAYOUT, echosounder_type$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int echosounder_type
     * }
     */
    public static void echosounder_type(MemorySegment struct, int fieldValue) {
        struct.set(echosounder_typeLAYOUT, echosounder_type$OFFSET, fieldValue);
    }

    private static final OfLong ping_numberLAYOUT = (OfLong)LAYOUT.select(groupElement("ping_number"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * long ping_number
     * }
     */
    public static final OfLong ping_numberLAYOUT() {
        return ping_numberLAYOUT;
    }

    private static final long ping_number$OFFSET = 32;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * long ping_number
     * }
     */
    public static final long ping_number$offset() {
        return ping_number$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * long ping_number
     * }
     */
    public static long ping_number(MemorySegment struct) {
        return struct.get(ping_numberLAYOUT, ping_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * long ping_number
     * }
     */
    public static void ping_number(MemorySegment struct, long fieldValue) {
        struct.set(ping_numberLAYOUT, ping_number$OFFSET, fieldValue);
    }

    private static final OfInt num_nav_samplesLAYOUT = (OfInt)LAYOUT.select(groupElement("num_nav_samples"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int num_nav_samples
     * }
     */
    public static final OfInt num_nav_samplesLAYOUT() {
        return num_nav_samplesLAYOUT;
    }

    private static final long num_nav_samples$OFFSET = 40;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int num_nav_samples
     * }
     */
    public static final long num_nav_samples$offset() {
        return num_nav_samples$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int num_nav_samples
     * }
     */
    public static int num_nav_samples(MemorySegment struct) {
        return struct.get(num_nav_samplesLAYOUT, num_nav_samples$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int num_nav_samples
     * }
     */
    public static void num_nav_samples(MemorySegment struct, int fieldValue) {
        struct.set(num_nav_samplesLAYOUT, num_nav_samples$OFFSET, fieldValue);
    }

    private static final OfInt num_attitude_samplesLAYOUT = (OfInt)LAYOUT.select(groupElement("num_attitude_samples"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int num_attitude_samples
     * }
     */
    public static final OfInt num_attitude_samplesLAYOUT() {
        return num_attitude_samplesLAYOUT;
    }

    private static final long num_attitude_samples$OFFSET = 44;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int num_attitude_samples
     * }
     */
    public static final long num_attitude_samples$offset() {
        return num_attitude_samples$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int num_attitude_samples
     * }
     */
    public static int num_attitude_samples(MemorySegment struct) {
        return struct.get(num_attitude_samplesLAYOUT, num_attitude_samples$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int num_attitude_samples
     * }
     */
    public static void num_attitude_samples(MemorySegment struct, int fieldValue) {
        struct.set(num_attitude_samplesLAYOUT, num_attitude_samples$OFFSET, fieldValue);
    }

    private static final OfInt num_heading_samplesLAYOUT = (OfInt)LAYOUT.select(groupElement("num_heading_samples"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int num_heading_samples
     * }
     */
    public static final OfInt num_heading_samplesLAYOUT() {
        return num_heading_samplesLAYOUT;
    }

    private static final long num_heading_samples$OFFSET = 48;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int num_heading_samples
     * }
     */
    public static final long num_heading_samples$offset() {
        return num_heading_samples$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int num_heading_samples
     * }
     */
    public static int num_heading_samples(MemorySegment struct) {
        return struct.get(num_heading_samplesLAYOUT, num_heading_samples$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int num_heading_samples
     * }
     */
    public static void num_heading_samples(MemorySegment struct, int fieldValue) {
        struct.set(num_heading_samplesLAYOUT, num_heading_samples$OFFSET, fieldValue);
    }

    private static final OfInt num_miniSVS_samplesLAYOUT = (OfInt)LAYOUT.select(groupElement("num_miniSVS_samples"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int num_miniSVS_samples
     * }
     */
    public static final OfInt num_miniSVS_samplesLAYOUT() {
        return num_miniSVS_samplesLAYOUT;
    }

    private static final long num_miniSVS_samples$OFFSET = 52;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int num_miniSVS_samples
     * }
     */
    public static final long num_miniSVS_samples$offset() {
        return num_miniSVS_samples$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int num_miniSVS_samples
     * }
     */
    public static int num_miniSVS_samples(MemorySegment struct) {
        return struct.get(num_miniSVS_samplesLAYOUT, num_miniSVS_samples$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int num_miniSVS_samples
     * }
     */
    public static void num_miniSVS_samples(MemorySegment struct, int fieldValue) {
        struct.set(num_miniSVS_samplesLAYOUT, num_miniSVS_samples$OFFSET, fieldValue);
    }

    private static final OfInt num_echosounder_samplesLAYOUT = (OfInt)LAYOUT.select(groupElement("num_echosounder_samples"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int num_echosounder_samples
     * }
     */
    public static final OfInt num_echosounder_samplesLAYOUT() {
        return num_echosounder_samplesLAYOUT;
    }

    private static final long num_echosounder_samples$OFFSET = 56;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int num_echosounder_samples
     * }
     */
    public static final long num_echosounder_samples$offset() {
        return num_echosounder_samples$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int num_echosounder_samples
     * }
     */
    public static int num_echosounder_samples(MemorySegment struct) {
        return struct.get(num_echosounder_samplesLAYOUT, num_echosounder_samples$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int num_echosounder_samples
     * }
     */
    public static void num_echosounder_samples(MemorySegment struct, int fieldValue) {
        struct.set(num_echosounder_samplesLAYOUT, num_echosounder_samples$OFFSET, fieldValue);
    }

    private static final OfInt num_raa_samplesLAYOUT = (OfInt)LAYOUT.select(groupElement("num_raa_samples"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int num_raa_samples
     * }
     */
    public static final OfInt num_raa_samplesLAYOUT() {
        return num_raa_samplesLAYOUT;
    }

    private static final long num_raa_samples$OFFSET = 60;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int num_raa_samples
     * }
     */
    public static final long num_raa_samples$offset() {
        return num_raa_samples$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int num_raa_samples
     * }
     */
    public static int num_raa_samples(MemorySegment struct) {
        return struct.get(num_raa_samplesLAYOUT, num_raa_samples$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int num_raa_samples
     * }
     */
    public static void num_raa_samples(MemorySegment struct, int fieldValue) {
        struct.set(num_raa_samplesLAYOUT, num_raa_samples$OFFSET, fieldValue);
    }

    private static final OfDouble mean_svLAYOUT = (OfDouble)LAYOUT.select(groupElement("mean_sv"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double mean_sv
     * }
     */
    public static final OfDouble mean_svLAYOUT() {
        return mean_svLAYOUT;
    }

    private static final long mean_sv$OFFSET = 64;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double mean_sv
     * }
     */
    public static final long mean_sv$offset() {
        return mean_sv$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double mean_sv
     * }
     */
    public static double mean_sv(MemorySegment struct) {
        return struct.get(mean_svLAYOUT, mean_sv$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double mean_sv
     * }
     */
    public static void mean_sv(MemorySegment struct, double fieldValue) {
        struct.set(mean_svLAYOUT, mean_sv$OFFSET, fieldValue);
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

    private static final long surface_velocity$OFFSET = 72;

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

    private static final OfInt valid_beamsLAYOUT = (OfInt)LAYOUT.select(groupElement("valid_beams"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int valid_beams
     * }
     */
    public static final OfInt valid_beamsLAYOUT() {
        return valid_beamsLAYOUT;
    }

    private static final long valid_beams$OFFSET = 80;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int valid_beams
     * }
     */
    public static final long valid_beams$offset() {
        return valid_beams$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int valid_beams
     * }
     */
    public static int valid_beams(MemorySegment struct) {
        return struct.get(valid_beamsLAYOUT, valid_beams$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int valid_beams
     * }
     */
    public static void valid_beams(MemorySegment struct, int fieldValue) {
        struct.set(valid_beamsLAYOUT, valid_beams$OFFSET, fieldValue);
    }

    private static final OfDouble sample_rateLAYOUT = (OfDouble)LAYOUT.select(groupElement("sample_rate"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double sample_rate
     * }
     */
    public static final OfDouble sample_rateLAYOUT() {
        return sample_rateLAYOUT;
    }

    private static final long sample_rate$OFFSET = 88;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double sample_rate
     * }
     */
    public static final long sample_rate$offset() {
        return sample_rate$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double sample_rate
     * }
     */
    public static double sample_rate(MemorySegment struct) {
        return struct.get(sample_rateLAYOUT, sample_rate$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double sample_rate
     * }
     */
    public static void sample_rate(MemorySegment struct, double fieldValue) {
        struct.set(sample_rateLAYOUT, sample_rate$OFFSET, fieldValue);
    }

    private static final OfDouble pulse_lengthLAYOUT = (OfDouble)LAYOUT.select(groupElement("pulse_length"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double pulse_length
     * }
     */
    public static final OfDouble pulse_lengthLAYOUT() {
        return pulse_lengthLAYOUT;
    }

    private static final long pulse_length$OFFSET = 96;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double pulse_length
     * }
     */
    public static final long pulse_length$offset() {
        return pulse_length$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double pulse_length
     * }
     */
    public static double pulse_length(MemorySegment struct) {
        return struct.get(pulse_lengthLAYOUT, pulse_length$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double pulse_length
     * }
     */
    public static void pulse_length(MemorySegment struct, double fieldValue) {
        struct.set(pulse_lengthLAYOUT, pulse_length$OFFSET, fieldValue);
    }

    private static final OfInt ping_lengthLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_length"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int ping_length
     * }
     */
    public static final OfInt ping_lengthLAYOUT() {
        return ping_lengthLAYOUT;
    }

    private static final long ping_length$OFFSET = 104;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int ping_length
     * }
     */
    public static final long ping_length$offset() {
        return ping_length$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_length
     * }
     */
    public static int ping_length(MemorySegment struct) {
        return struct.get(ping_lengthLAYOUT, ping_length$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_length
     * }
     */
    public static void ping_length(MemorySegment struct, int fieldValue) {
        struct.set(ping_lengthLAYOUT, ping_length$OFFSET, fieldValue);
    }

    private static final OfInt transmit_powerLAYOUT = (OfInt)LAYOUT.select(groupElement("transmit_power"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int transmit_power
     * }
     */
    public static final OfInt transmit_powerLAYOUT() {
        return transmit_powerLAYOUT;
    }

    private static final long transmit_power$OFFSET = 108;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int transmit_power
     * }
     */
    public static final long transmit_power$offset() {
        return transmit_power$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int transmit_power
     * }
     */
    public static int transmit_power(MemorySegment struct) {
        return struct.get(transmit_powerLAYOUT, transmit_power$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int transmit_power
     * }
     */
    public static void transmit_power(MemorySegment struct, int fieldValue) {
        struct.set(transmit_powerLAYOUT, transmit_power$OFFSET, fieldValue);
    }

    private static final OfInt sidescan_gain_channelLAYOUT = (OfInt)LAYOUT.select(groupElement("sidescan_gain_channel"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int sidescan_gain_channel
     * }
     */
    public static final OfInt sidescan_gain_channelLAYOUT() {
        return sidescan_gain_channelLAYOUT;
    }

    private static final long sidescan_gain_channel$OFFSET = 112;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int sidescan_gain_channel
     * }
     */
    public static final long sidescan_gain_channel$offset() {
        return sidescan_gain_channel$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int sidescan_gain_channel
     * }
     */
    public static int sidescan_gain_channel(MemorySegment struct) {
        return struct.get(sidescan_gain_channelLAYOUT, sidescan_gain_channel$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int sidescan_gain_channel
     * }
     */
    public static void sidescan_gain_channel(MemorySegment struct, int fieldValue) {
        struct.set(sidescan_gain_channelLAYOUT, sidescan_gain_channel$OFFSET, fieldValue);
    }

    private static final OfInt stabilizationLAYOUT = (OfInt)LAYOUT.select(groupElement("stabilization"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int stabilization
     * }
     */
    public static final OfInt stabilizationLAYOUT() {
        return stabilizationLAYOUT;
    }

    private static final long stabilization$OFFSET = 116;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int stabilization
     * }
     */
    public static final long stabilization$offset() {
        return stabilization$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int stabilization
     * }
     */
    public static int stabilization(MemorySegment struct) {
        return struct.get(stabilizationLAYOUT, stabilization$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int stabilization
     * }
     */
    public static void stabilization(MemorySegment struct, int fieldValue) {
        struct.set(stabilizationLAYOUT, stabilization$OFFSET, fieldValue);
    }

    private static final OfInt gps_qualityLAYOUT = (OfInt)LAYOUT.select(groupElement("gps_quality"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int gps_quality
     * }
     */
    public static final OfInt gps_qualityLAYOUT() {
        return gps_qualityLAYOUT;
    }

    private static final long gps_quality$OFFSET = 120;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int gps_quality
     * }
     */
    public static final long gps_quality$offset() {
        return gps_quality$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int gps_quality
     * }
     */
    public static int gps_quality(MemorySegment struct) {
        return struct.get(gps_qualityLAYOUT, gps_quality$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int gps_quality
     * }
     */
    public static void gps_quality(MemorySegment struct, int fieldValue) {
        struct.set(gps_qualityLAYOUT, gps_quality$OFFSET, fieldValue);
    }

    private static final OfDouble range_uncertaintyLAYOUT = (OfDouble)LAYOUT.select(groupElement("range_uncertainty"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double range_uncertainty
     * }
     */
    public static final OfDouble range_uncertaintyLAYOUT() {
        return range_uncertaintyLAYOUT;
    }

    private static final long range_uncertainty$OFFSET = 128;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double range_uncertainty
     * }
     */
    public static final long range_uncertainty$offset() {
        return range_uncertainty$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double range_uncertainty
     * }
     */
    public static double range_uncertainty(MemorySegment struct) {
        return struct.get(range_uncertaintyLAYOUT, range_uncertainty$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double range_uncertainty
     * }
     */
    public static void range_uncertainty(MemorySegment struct, double fieldValue) {
        struct.set(range_uncertaintyLAYOUT, range_uncertainty$OFFSET, fieldValue);
    }

    private static final OfDouble angle_uncertaintyLAYOUT = (OfDouble)LAYOUT.select(groupElement("angle_uncertainty"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double angle_uncertainty
     * }
     */
    public static final OfDouble angle_uncertaintyLAYOUT() {
        return angle_uncertaintyLAYOUT;
    }

    private static final long angle_uncertainty$OFFSET = 136;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double angle_uncertainty
     * }
     */
    public static final long angle_uncertainty$offset() {
        return angle_uncertainty$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double angle_uncertainty
     * }
     */
    public static double angle_uncertainty(MemorySegment struct) {
        return struct.get(angle_uncertaintyLAYOUT, angle_uncertainty$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double angle_uncertainty
     * }
     */
    public static void angle_uncertainty(MemorySegment struct, double fieldValue) {
        struct.set(angle_uncertaintyLAYOUT, angle_uncertainty$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * char spare[32]
     * }
     */
    public static final SequenceLayout spareLAYOUT() {
        return spareLAYOUT;
    }

    private static final long spare$OFFSET = 144;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * char spare[32]
     * }
     */
    public static final long spare$offset() {
        return spare$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char spare[32]
     * }
     */
    public static MemorySegment spare(MemorySegment struct) {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char spare[32]
     * }
     */
    public static void spare(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static long[] spare$DIMS = { 32 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * char spare[32]
     * }
     */
    public static long[] spare$dimensions() {
        return spare$DIMS;
    }
    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * char spare[32]
     * }
     */
    public static byte spare(MemorySegment struct, long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * char spare[32]
     * }
     */
    public static void spare(MemorySegment struct, long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

}

