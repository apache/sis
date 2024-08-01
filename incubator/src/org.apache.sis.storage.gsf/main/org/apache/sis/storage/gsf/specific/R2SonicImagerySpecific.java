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
import org.apache.sis.storage.gsf.TimeSpec;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class R2SonicImagerySpecific extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        MemoryLayout.sequenceLayout(12, GSF.C_CHAR).withName("model_number"),
        MemoryLayout.sequenceLayout(12, GSF.C_CHAR).withName("serial_number"),
        TimeSpec.LAYOUT.withName("dg_time"),
        GSF.C_INT.withName("ping_number"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("ping_period"),
        GSF.C_DOUBLE.withName("sound_speed"),
        GSF.C_DOUBLE.withName("frequency"),
        GSF.C_DOUBLE.withName("tx_power"),
        GSF.C_DOUBLE.withName("tx_pulse_width"),
        GSF.C_DOUBLE.withName("tx_beamwidth_vert"),
        GSF.C_DOUBLE.withName("tx_beamwidth_horiz"),
        GSF.C_DOUBLE.withName("tx_steering_vert"),
        GSF.C_DOUBLE.withName("tx_steering_horiz"),
        GSF.C_INT.withName("tx_misc_info"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("rx_bandwidth"),
        GSF.C_DOUBLE.withName("rx_sample_rate"),
        GSF.C_DOUBLE.withName("rx_range"),
        GSF.C_DOUBLE.withName("rx_gain"),
        GSF.C_DOUBLE.withName("rx_spreading"),
        GSF.C_DOUBLE.withName("rx_absorption"),
        GSF.C_DOUBLE.withName("rx_mount_tilt"),
        GSF.C_INT.withName("rx_misc_info"),
        GSF.C_SHORT.withName("reserved"),
        GSF.C_SHORT.withName("num_beams"),
        MemoryLayout.sequenceLayout(6, GSF.C_DOUBLE).withName("more_info"),
        MemoryLayout.sequenceLayout(32, GSF.C_CHAR).withName("spare")
    ).withName("t_gsfR2SonicImagerySpecific");

    public R2SonicImagerySpecific(MemorySegment struct) {
        super(struct);
    }

    public R2SonicImagerySpecific(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final SequenceLayout model_numberLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("model_number"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char model_number[12]
     * }
     */
    public static final SequenceLayout model_numberLAYOUT() {
        return model_numberLAYOUT;
    }

    private static final long model_number$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char model_number[12]
     * }
     */
    public static final long model_number$offset() {
        return model_number$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char model_number[12]
     * }
     */
    public static MemorySegment model_number(MemorySegment struct) {
        return struct.asSlice(model_number$OFFSET, model_numberLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char model_number[12]
     * }
     */
    public static void model_number(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, model_number$OFFSET, model_numberLAYOUT.byteSize());
    }

    private static long[] model_number$DIMS = { 12 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * unsigned char model_number[12]
     * }
     */
    public static long[] model_number$dimensions() {
        return model_number$DIMS;
    }
    private static final VarHandle model_number$ELEM_HANDLE = model_numberLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char model_number[12]
     * }
     */
    public static byte model_number(MemorySegment struct, long index0) {
        return (byte)model_number$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char model_number[12]
     * }
     */
    public static void model_number(MemorySegment struct, long index0, byte fieldValue) {
        model_number$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final SequenceLayout serial_numberLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("serial_number"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char serial_number[12]
     * }
     */
    public static final SequenceLayout serial_numberLAYOUT() {
        return serial_numberLAYOUT;
    }

    private static final long serial_number$OFFSET = 12;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char serial_number[12]
     * }
     */
    public static final long serial_number$offset() {
        return serial_number$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char serial_number[12]
     * }
     */
    public static MemorySegment serial_number(MemorySegment struct) {
        return struct.asSlice(serial_number$OFFSET, serial_numberLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char serial_number[12]
     * }
     */
    public static void serial_number(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, serial_number$OFFSET, serial_numberLAYOUT.byteSize());
    }

    private static long[] serial_number$DIMS = { 12 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * unsigned char serial_number[12]
     * }
     */
    public static long[] serial_number$dimensions() {
        return serial_number$DIMS;
    }
    private static final VarHandle serial_number$ELEM_HANDLE = serial_numberLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char serial_number[12]
     * }
     */
    public static byte serial_number(MemorySegment struct, long index0) {
        return (byte)serial_number$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char serial_number[12]
     * }
     */
    public static void serial_number(MemorySegment struct, long index0, byte fieldValue) {
        serial_number$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final GroupLayout dg_timeLAYOUT = (GroupLayout)LAYOUT.select(groupElement("dg_time"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * struct timespec dg_time
     * }
     */
    public static final GroupLayout dg_timeLAYOUT() {
        return dg_timeLAYOUT;
    }

    private static final long dg_time$OFFSET = 24;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * struct timespec dg_time
     * }
     */
    public static final long dg_time$offset() {
        return dg_time$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * struct timespec dg_time
     * }
     */
    public static MemorySegment dg_time(MemorySegment struct) {
        return struct.asSlice(dg_time$OFFSET, dg_timeLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * struct timespec dg_time
     * }
     */
    public static void dg_time(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, dg_time$OFFSET, dg_timeLAYOUT.byteSize());
    }

    private static final OfInt ping_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_number"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned int ping_number
     * }
     */
    public static final OfInt ping_numberLAYOUT() {
        return ping_numberLAYOUT;
    }

    private static final long ping_number$OFFSET = 40;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned int ping_number
     * }
     */
    public static final long ping_number$offset() {
        return ping_number$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int ping_number
     * }
     */
    public static int ping_number(MemorySegment struct) {
        return struct.get(ping_numberLAYOUT, ping_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int ping_number
     * }
     */
    public static void ping_number(MemorySegment struct, int fieldValue) {
        struct.set(ping_numberLAYOUT, ping_number$OFFSET, fieldValue);
    }

    private static final OfDouble ping_periodLAYOUT = (OfDouble)LAYOUT.select(groupElement("ping_period"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double ping_period
     * }
     */
    public static final OfDouble ping_periodLAYOUT() {
        return ping_periodLAYOUT;
    }

    private static final long ping_period$OFFSET = 48;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double ping_period
     * }
     */
    public static final long ping_period$offset() {
        return ping_period$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double ping_period
     * }
     */
    public static double ping_period(MemorySegment struct) {
        return struct.get(ping_periodLAYOUT, ping_period$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double ping_period
     * }
     */
    public static void ping_period(MemorySegment struct, double fieldValue) {
        struct.set(ping_periodLAYOUT, ping_period$OFFSET, fieldValue);
    }

    private static final OfDouble sound_speedLAYOUT = (OfDouble)LAYOUT.select(groupElement("sound_speed"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double sound_speed
     * }
     */
    public static final OfDouble sound_speedLAYOUT() {
        return sound_speedLAYOUT;
    }

    private static final long sound_speed$OFFSET = 56;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double sound_speed
     * }
     */
    public static final long sound_speed$offset() {
        return sound_speed$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double sound_speed
     * }
     */
    public static double sound_speed(MemorySegment struct) {
        return struct.get(sound_speedLAYOUT, sound_speed$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double sound_speed
     * }
     */
    public static void sound_speed(MemorySegment struct, double fieldValue) {
        struct.set(sound_speedLAYOUT, sound_speed$OFFSET, fieldValue);
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

    private static final long frequency$OFFSET = 64;

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

    private static final OfDouble tx_powerLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_power"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double tx_power
     * }
     */
    public static final OfDouble tx_powerLAYOUT() {
        return tx_powerLAYOUT;
    }

    private static final long tx_power$OFFSET = 72;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double tx_power
     * }
     */
    public static final long tx_power$offset() {
        return tx_power$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_power
     * }
     */
    public static double tx_power(MemorySegment struct) {
        return struct.get(tx_powerLAYOUT, tx_power$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_power
     * }
     */
    public static void tx_power(MemorySegment struct, double fieldValue) {
        struct.set(tx_powerLAYOUT, tx_power$OFFSET, fieldValue);
    }

    private static final OfDouble tx_pulse_widthLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_pulse_width"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double tx_pulse_width
     * }
     */
    public static final OfDouble tx_pulse_widthLAYOUT() {
        return tx_pulse_widthLAYOUT;
    }

    private static final long tx_pulse_width$OFFSET = 80;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double tx_pulse_width
     * }
     */
    public static final long tx_pulse_width$offset() {
        return tx_pulse_width$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_pulse_width
     * }
     */
    public static double tx_pulse_width(MemorySegment struct) {
        return struct.get(tx_pulse_widthLAYOUT, tx_pulse_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_pulse_width
     * }
     */
    public static void tx_pulse_width(MemorySegment struct, double fieldValue) {
        struct.set(tx_pulse_widthLAYOUT, tx_pulse_width$OFFSET, fieldValue);
    }

    private static final OfDouble tx_beamwidth_vertLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_beamwidth_vert"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double tx_beamwidth_vert
     * }
     */
    public static final OfDouble tx_beamwidth_vertLAYOUT() {
        return tx_beamwidth_vertLAYOUT;
    }

    private static final long tx_beamwidth_vert$OFFSET = 88;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double tx_beamwidth_vert
     * }
     */
    public static final long tx_beamwidth_vert$offset() {
        return tx_beamwidth_vert$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_beamwidth_vert
     * }
     */
    public static double tx_beamwidth_vert(MemorySegment struct) {
        return struct.get(tx_beamwidth_vertLAYOUT, tx_beamwidth_vert$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_beamwidth_vert
     * }
     */
    public static void tx_beamwidth_vert(MemorySegment struct, double fieldValue) {
        struct.set(tx_beamwidth_vertLAYOUT, tx_beamwidth_vert$OFFSET, fieldValue);
    }

    private static final OfDouble tx_beamwidth_horizLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_beamwidth_horiz"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double tx_beamwidth_horiz
     * }
     */
    public static final OfDouble tx_beamwidth_horizLAYOUT() {
        return tx_beamwidth_horizLAYOUT;
    }

    private static final long tx_beamwidth_horiz$OFFSET = 96;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double tx_beamwidth_horiz
     * }
     */
    public static final long tx_beamwidth_horiz$offset() {
        return tx_beamwidth_horiz$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_beamwidth_horiz
     * }
     */
    public static double tx_beamwidth_horiz(MemorySegment struct) {
        return struct.get(tx_beamwidth_horizLAYOUT, tx_beamwidth_horiz$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_beamwidth_horiz
     * }
     */
    public static void tx_beamwidth_horiz(MemorySegment struct, double fieldValue) {
        struct.set(tx_beamwidth_horizLAYOUT, tx_beamwidth_horiz$OFFSET, fieldValue);
    }

    private static final OfDouble tx_steering_vertLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_steering_vert"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double tx_steering_vert
     * }
     */
    public static final OfDouble tx_steering_vertLAYOUT() {
        return tx_steering_vertLAYOUT;
    }

    private static final long tx_steering_vert$OFFSET = 104;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double tx_steering_vert
     * }
     */
    public static final long tx_steering_vert$offset() {
        return tx_steering_vert$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_steering_vert
     * }
     */
    public static double tx_steering_vert(MemorySegment struct) {
        return struct.get(tx_steering_vertLAYOUT, tx_steering_vert$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_steering_vert
     * }
     */
    public static void tx_steering_vert(MemorySegment struct, double fieldValue) {
        struct.set(tx_steering_vertLAYOUT, tx_steering_vert$OFFSET, fieldValue);
    }

    private static final OfDouble tx_steering_horizLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_steering_horiz"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double tx_steering_horiz
     * }
     */
    public static final OfDouble tx_steering_horizLAYOUT() {
        return tx_steering_horizLAYOUT;
    }

    private static final long tx_steering_horiz$OFFSET = 112;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double tx_steering_horiz
     * }
     */
    public static final long tx_steering_horiz$offset() {
        return tx_steering_horiz$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_steering_horiz
     * }
     */
    public static double tx_steering_horiz(MemorySegment struct) {
        return struct.get(tx_steering_horizLAYOUT, tx_steering_horiz$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_steering_horiz
     * }
     */
    public static void tx_steering_horiz(MemorySegment struct, double fieldValue) {
        struct.set(tx_steering_horizLAYOUT, tx_steering_horiz$OFFSET, fieldValue);
    }

    private static final OfInt tx_misc_infoLAYOUT = (OfInt)LAYOUT.select(groupElement("tx_misc_info"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned int tx_misc_info
     * }
     */
    public static final OfInt tx_misc_infoLAYOUT() {
        return tx_misc_infoLAYOUT;
    }

    private static final long tx_misc_info$OFFSET = 120;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned int tx_misc_info
     * }
     */
    public static final long tx_misc_info$offset() {
        return tx_misc_info$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int tx_misc_info
     * }
     */
    public static int tx_misc_info(MemorySegment struct) {
        return struct.get(tx_misc_infoLAYOUT, tx_misc_info$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int tx_misc_info
     * }
     */
    public static void tx_misc_info(MemorySegment struct, int fieldValue) {
        struct.set(tx_misc_infoLAYOUT, tx_misc_info$OFFSET, fieldValue);
    }

    private static final OfDouble rx_bandwidthLAYOUT = (OfDouble)LAYOUT.select(groupElement("rx_bandwidth"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double rx_bandwidth
     * }
     */
    public static final OfDouble rx_bandwidthLAYOUT() {
        return rx_bandwidthLAYOUT;
    }

    private static final long rx_bandwidth$OFFSET = 128;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double rx_bandwidth
     * }
     */
    public static final long rx_bandwidth$offset() {
        return rx_bandwidth$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double rx_bandwidth
     * }
     */
    public static double rx_bandwidth(MemorySegment struct) {
        return struct.get(rx_bandwidthLAYOUT, rx_bandwidth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double rx_bandwidth
     * }
     */
    public static void rx_bandwidth(MemorySegment struct, double fieldValue) {
        struct.set(rx_bandwidthLAYOUT, rx_bandwidth$OFFSET, fieldValue);
    }

    private static final OfDouble rx_sample_rateLAYOUT = (OfDouble)LAYOUT.select(groupElement("rx_sample_rate"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double rx_sample_rate
     * }
     */
    public static final OfDouble rx_sample_rateLAYOUT() {
        return rx_sample_rateLAYOUT;
    }

    private static final long rx_sample_rate$OFFSET = 136;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double rx_sample_rate
     * }
     */
    public static final long rx_sample_rate$offset() {
        return rx_sample_rate$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double rx_sample_rate
     * }
     */
    public static double rx_sample_rate(MemorySegment struct) {
        return struct.get(rx_sample_rateLAYOUT, rx_sample_rate$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double rx_sample_rate
     * }
     */
    public static void rx_sample_rate(MemorySegment struct, double fieldValue) {
        struct.set(rx_sample_rateLAYOUT, rx_sample_rate$OFFSET, fieldValue);
    }

    private static final OfDouble rx_rangeLAYOUT = (OfDouble)LAYOUT.select(groupElement("rx_range"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double rx_range
     * }
     */
    public static final OfDouble rx_rangeLAYOUT() {
        return rx_rangeLAYOUT;
    }

    private static final long rx_range$OFFSET = 144;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double rx_range
     * }
     */
    public static final long rx_range$offset() {
        return rx_range$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double rx_range
     * }
     */
    public static double rx_range(MemorySegment struct) {
        return struct.get(rx_rangeLAYOUT, rx_range$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double rx_range
     * }
     */
    public static void rx_range(MemorySegment struct, double fieldValue) {
        struct.set(rx_rangeLAYOUT, rx_range$OFFSET, fieldValue);
    }

    private static final OfDouble rx_gainLAYOUT = (OfDouble)LAYOUT.select(groupElement("rx_gain"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double rx_gain
     * }
     */
    public static final OfDouble rx_gainLAYOUT() {
        return rx_gainLAYOUT;
    }

    private static final long rx_gain$OFFSET = 152;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double rx_gain
     * }
     */
    public static final long rx_gain$offset() {
        return rx_gain$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double rx_gain
     * }
     */
    public static double rx_gain(MemorySegment struct) {
        return struct.get(rx_gainLAYOUT, rx_gain$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double rx_gain
     * }
     */
    public static void rx_gain(MemorySegment struct, double fieldValue) {
        struct.set(rx_gainLAYOUT, rx_gain$OFFSET, fieldValue);
    }

    private static final OfDouble rx_spreadingLAYOUT = (OfDouble)LAYOUT.select(groupElement("rx_spreading"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double rx_spreading
     * }
     */
    public static final OfDouble rx_spreadingLAYOUT() {
        return rx_spreadingLAYOUT;
    }

    private static final long rx_spreading$OFFSET = 160;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double rx_spreading
     * }
     */
    public static final long rx_spreading$offset() {
        return rx_spreading$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double rx_spreading
     * }
     */
    public static double rx_spreading(MemorySegment struct) {
        return struct.get(rx_spreadingLAYOUT, rx_spreading$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double rx_spreading
     * }
     */
    public static void rx_spreading(MemorySegment struct, double fieldValue) {
        struct.set(rx_spreadingLAYOUT, rx_spreading$OFFSET, fieldValue);
    }

    private static final OfDouble rx_absorptionLAYOUT = (OfDouble)LAYOUT.select(groupElement("rx_absorption"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double rx_absorption
     * }
     */
    public static final OfDouble rx_absorptionLAYOUT() {
        return rx_absorptionLAYOUT;
    }

    private static final long rx_absorption$OFFSET = 168;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double rx_absorption
     * }
     */
    public static final long rx_absorption$offset() {
        return rx_absorption$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double rx_absorption
     * }
     */
    public static double rx_absorption(MemorySegment struct) {
        return struct.get(rx_absorptionLAYOUT, rx_absorption$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double rx_absorption
     * }
     */
    public static void rx_absorption(MemorySegment struct, double fieldValue) {
        struct.set(rx_absorptionLAYOUT, rx_absorption$OFFSET, fieldValue);
    }

    private static final OfDouble rx_mount_tiltLAYOUT = (OfDouble)LAYOUT.select(groupElement("rx_mount_tilt"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double rx_mount_tilt
     * }
     */
    public static final OfDouble rx_mount_tiltLAYOUT() {
        return rx_mount_tiltLAYOUT;
    }

    private static final long rx_mount_tilt$OFFSET = 176;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double rx_mount_tilt
     * }
     */
    public static final long rx_mount_tilt$offset() {
        return rx_mount_tilt$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double rx_mount_tilt
     * }
     */
    public static double rx_mount_tilt(MemorySegment struct) {
        return struct.get(rx_mount_tiltLAYOUT, rx_mount_tilt$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double rx_mount_tilt
     * }
     */
    public static void rx_mount_tilt(MemorySegment struct, double fieldValue) {
        struct.set(rx_mount_tiltLAYOUT, rx_mount_tilt$OFFSET, fieldValue);
    }

    private static final OfInt rx_misc_infoLAYOUT = (OfInt)LAYOUT.select(groupElement("rx_misc_info"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned int rx_misc_info
     * }
     */
    public static final OfInt rx_misc_infoLAYOUT() {
        return rx_misc_infoLAYOUT;
    }

    private static final long rx_misc_info$OFFSET = 184;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned int rx_misc_info
     * }
     */
    public static final long rx_misc_info$offset() {
        return rx_misc_info$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int rx_misc_info
     * }
     */
    public static int rx_misc_info(MemorySegment struct) {
        return struct.get(rx_misc_infoLAYOUT, rx_misc_info$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int rx_misc_info
     * }
     */
    public static void rx_misc_info(MemorySegment struct, int fieldValue) {
        struct.set(rx_misc_infoLAYOUT, rx_misc_info$OFFSET, fieldValue);
    }

    private static final OfShort reservedLAYOUT = (OfShort)LAYOUT.select(groupElement("reserved"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned short reserved
     * }
     */
    public static final OfShort reservedLAYOUT() {
        return reservedLAYOUT;
    }

    private static final long reserved$OFFSET = 188;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned short reserved
     * }
     */
    public static final long reserved$offset() {
        return reserved$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short reserved
     * }
     */
    public static short reserved(MemorySegment struct) {
        return struct.get(reservedLAYOUT, reserved$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short reserved
     * }
     */
    public static void reserved(MemorySegment struct, short fieldValue) {
        struct.set(reservedLAYOUT, reserved$OFFSET, fieldValue);
    }

    private static final OfShort num_beamsLAYOUT = (OfShort)LAYOUT.select(groupElement("num_beams"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned short num_beams
     * }
     */
    public static final OfShort num_beamsLAYOUT() {
        return num_beamsLAYOUT;
    }

    private static final long num_beams$OFFSET = 190;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned short num_beams
     * }
     */
    public static final long num_beams$offset() {
        return num_beams$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short num_beams
     * }
     */
    public static short num_beams(MemorySegment struct) {
        return struct.get(num_beamsLAYOUT, num_beams$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short num_beams
     * }
     */
    public static void num_beams(MemorySegment struct, short fieldValue) {
        struct.set(num_beamsLAYOUT, num_beams$OFFSET, fieldValue);
    }

    private static final SequenceLayout more_infoLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("more_info"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double more_info[6]
     * }
     */
    public static final SequenceLayout more_infoLAYOUT() {
        return more_infoLAYOUT;
    }

    private static final long more_info$OFFSET = 192;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double more_info[6]
     * }
     */
    public static final long more_info$offset() {
        return more_info$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double more_info[6]
     * }
     */
    public static MemorySegment more_info(MemorySegment struct) {
        return struct.asSlice(more_info$OFFSET, more_infoLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double more_info[6]
     * }
     */
    public static void more_info(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, more_info$OFFSET, more_infoLAYOUT.byteSize());
    }

    private static long[] more_info$DIMS = { 6 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * double more_info[6]
     * }
     */
    public static long[] more_info$dimensions() {
        return more_info$DIMS;
    }
    private static final VarHandle more_info$ELEM_HANDLE = more_infoLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * double more_info[6]
     * }
     */
    public static double more_info(MemorySegment struct, long index0) {
        return (double)more_info$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * double more_info[6]
     * }
     */
    public static void more_info(MemorySegment struct, long index0, double fieldValue) {
        more_info$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char spare[32]
     * }
     */
    public static final SequenceLayout spareLAYOUT() {
        return spareLAYOUT;
    }

    private static final long spare$OFFSET = 240;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char spare[32]
     * }
     */
    public static final long spare$offset() {
        return spare$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare[32]
     * }
     */
    public static MemorySegment spare(MemorySegment struct) {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char spare[32]
     * }
     */
    public static void spare(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static long[] spare$DIMS = { 32 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * unsigned char spare[32]
     * }
     */
    public static long[] spare$dimensions() {
        return spare$DIMS;
    }
    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char spare[32]
     * }
     */
    public static byte spare(MemorySegment struct, long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char spare[32]
     * }
     */
    public static void spare(MemorySegment struct, long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

}

