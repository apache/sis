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
import org.apache.sis.storage.gsf.TimeSpec;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class EM3RunTime extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("model_number"),
        MemoryLayout.paddingLayout(4),
        TimeSpec.LAYOUT.withName("dg_time"),
        GSF.C_INT.withName("ping_number"),
        GSF.C_INT.withName("serial_number"),
        GSF.C_INT.withName("system_status"),
        GSF.C_INT.withName("mode"),
        GSF.C_INT.withName("filter_id"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("min_depth"),
        GSF.C_DOUBLE.withName("max_depth"),
        GSF.C_DOUBLE.withName("absorption"),
        GSF.C_DOUBLE.withName("pulse_length"),
        GSF.C_DOUBLE.withName("transmit_beam_width"),
        GSF.C_INT.withName("power_reduction"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("receive_beam_width"),
        GSF.C_INT.withName("receive_bandwidth"),
        GSF.C_INT.withName("receive_gain"),
        GSF.C_INT.withName("cross_over_angle"),
        GSF.C_INT.withName("ssv_source"),
        GSF.C_INT.withName("swath_width"),
        GSF.C_INT.withName("beam_spacing"),
        GSF.C_INT.withName("coverage_sector"),
        GSF.C_INT.withName("stabilization"),
        GSF.C_INT.withName("port_swath_width"),
        GSF.C_INT.withName("stbd_swath_width"),
        GSF.C_INT.withName("port_coverage_sector"),
        GSF.C_INT.withName("stbd_coverage_sector"),
        GSF.C_INT.withName("hilo_freq_absorp_ratio"),
        GSF.C_INT.withName("spare1")
    ).withName("t_gsfEM3RunTime");

    public EM3RunTime(MemorySegment struct) {
        super(struct);
    }

    public EM3RunTime(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
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

    private static final long model_number$OFFSET = 0;

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

    private static final long dg_time$OFFSET = 8;

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
     * int ping_number
     * }
     */
    public static final OfInt ping_numberLAYOUT() {
        return ping_numberLAYOUT;
    }

    private static final long ping_number$OFFSET = 24;

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

    private static final OfInt serial_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("serial_number"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int serial_number
     * }
     */
    public static final OfInt serial_numberLAYOUT() {
        return serial_numberLAYOUT;
    }

    private static final long serial_number$OFFSET = 28;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int serial_number
     * }
     */
    public static final long serial_number$offset() {
        return serial_number$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int serial_number
     * }
     */
    public static int serial_number(MemorySegment struct) {
        return struct.get(serial_numberLAYOUT, serial_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int serial_number
     * }
     */
    public static void serial_number(MemorySegment struct, int fieldValue) {
        struct.set(serial_numberLAYOUT, serial_number$OFFSET, fieldValue);
    }

    private static final OfInt system_statusLAYOUT = (OfInt)LAYOUT.select(groupElement("system_status"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int system_status
     * }
     */
    public static final OfInt system_statusLAYOUT() {
        return system_statusLAYOUT;
    }

    private static final long system_status$OFFSET = 32;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int system_status
     * }
     */
    public static final long system_status$offset() {
        return system_status$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int system_status
     * }
     */
    public static int system_status(MemorySegment struct) {
        return struct.get(system_statusLAYOUT, system_status$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int system_status
     * }
     */
    public static void system_status(MemorySegment struct, int fieldValue) {
        struct.set(system_statusLAYOUT, system_status$OFFSET, fieldValue);
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

    private static final long mode$OFFSET = 36;

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

    private static final OfInt filter_idLAYOUT = (OfInt)LAYOUT.select(groupElement("filter_id"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int filter_id
     * }
     */
    public static final OfInt filter_idLAYOUT() {
        return filter_idLAYOUT;
    }

    private static final long filter_id$OFFSET = 40;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int filter_id
     * }
     */
    public static final long filter_id$offset() {
        return filter_id$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int filter_id
     * }
     */
    public static int filter_id(MemorySegment struct) {
        return struct.get(filter_idLAYOUT, filter_id$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int filter_id
     * }
     */
    public static void filter_id(MemorySegment struct, int fieldValue) {
        struct.set(filter_idLAYOUT, filter_id$OFFSET, fieldValue);
    }

    private static final OfDouble min_depthLAYOUT = (OfDouble)LAYOUT.select(groupElement("min_depth"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double min_depth
     * }
     */
    public static final OfDouble min_depthLAYOUT() {
        return min_depthLAYOUT;
    }

    private static final long min_depth$OFFSET = 48;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double min_depth
     * }
     */
    public static final long min_depth$offset() {
        return min_depth$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double min_depth
     * }
     */
    public static double min_depth(MemorySegment struct) {
        return struct.get(min_depthLAYOUT, min_depth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double min_depth
     * }
     */
    public static void min_depth(MemorySegment struct, double fieldValue) {
        struct.set(min_depthLAYOUT, min_depth$OFFSET, fieldValue);
    }

    private static final OfDouble max_depthLAYOUT = (OfDouble)LAYOUT.select(groupElement("max_depth"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double max_depth
     * }
     */
    public static final OfDouble max_depthLAYOUT() {
        return max_depthLAYOUT;
    }

    private static final long max_depth$OFFSET = 56;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double max_depth
     * }
     */
    public static final long max_depth$offset() {
        return max_depth$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double max_depth
     * }
     */
    public static double max_depth(MemorySegment struct) {
        return struct.get(max_depthLAYOUT, max_depth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double max_depth
     * }
     */
    public static void max_depth(MemorySegment struct, double fieldValue) {
        struct.set(max_depthLAYOUT, max_depth$OFFSET, fieldValue);
    }

    private static final OfDouble absorptionLAYOUT = (OfDouble)LAYOUT.select(groupElement("absorption"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double absorption
     * }
     */
    public static final OfDouble absorptionLAYOUT() {
        return absorptionLAYOUT;
    }

    private static final long absorption$OFFSET = 64;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double absorption
     * }
     */
    public static final long absorption$offset() {
        return absorption$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double absorption
     * }
     */
    public static double absorption(MemorySegment struct) {
        return struct.get(absorptionLAYOUT, absorption$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double absorption
     * }
     */
    public static void absorption(MemorySegment struct, double fieldValue) {
        struct.set(absorptionLAYOUT, absorption$OFFSET, fieldValue);
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

    private static final long pulse_length$OFFSET = 72;

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

    private static final OfDouble transmit_beam_widthLAYOUT = (OfDouble)LAYOUT.select(groupElement("transmit_beam_width"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double transmit_beam_width
     * }
     */
    public static final OfDouble transmit_beam_widthLAYOUT() {
        return transmit_beam_widthLAYOUT;
    }

    private static final long transmit_beam_width$OFFSET = 80;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double transmit_beam_width
     * }
     */
    public static final long transmit_beam_width$offset() {
        return transmit_beam_width$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double transmit_beam_width
     * }
     */
    public static double transmit_beam_width(MemorySegment struct) {
        return struct.get(transmit_beam_widthLAYOUT, transmit_beam_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double transmit_beam_width
     * }
     */
    public static void transmit_beam_width(MemorySegment struct, double fieldValue) {
        struct.set(transmit_beam_widthLAYOUT, transmit_beam_width$OFFSET, fieldValue);
    }

    private static final OfInt power_reductionLAYOUT = (OfInt)LAYOUT.select(groupElement("power_reduction"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int power_reduction
     * }
     */
    public static final OfInt power_reductionLAYOUT() {
        return power_reductionLAYOUT;
    }

    private static final long power_reduction$OFFSET = 88;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int power_reduction
     * }
     */
    public static final long power_reduction$offset() {
        return power_reduction$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int power_reduction
     * }
     */
    public static int power_reduction(MemorySegment struct) {
        return struct.get(power_reductionLAYOUT, power_reduction$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int power_reduction
     * }
     */
    public static void power_reduction(MemorySegment struct, int fieldValue) {
        struct.set(power_reductionLAYOUT, power_reduction$OFFSET, fieldValue);
    }

    private static final OfDouble receive_beam_widthLAYOUT = (OfDouble)LAYOUT.select(groupElement("receive_beam_width"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double receive_beam_width
     * }
     */
    public static final OfDouble receive_beam_widthLAYOUT() {
        return receive_beam_widthLAYOUT;
    }

    private static final long receive_beam_width$OFFSET = 96;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double receive_beam_width
     * }
     */
    public static final long receive_beam_width$offset() {
        return receive_beam_width$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double receive_beam_width
     * }
     */
    public static double receive_beam_width(MemorySegment struct) {
        return struct.get(receive_beam_widthLAYOUT, receive_beam_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double receive_beam_width
     * }
     */
    public static void receive_beam_width(MemorySegment struct, double fieldValue) {
        struct.set(receive_beam_widthLAYOUT, receive_beam_width$OFFSET, fieldValue);
    }

    private static final OfInt receive_bandwidthLAYOUT = (OfInt)LAYOUT.select(groupElement("receive_bandwidth"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int receive_bandwidth
     * }
     */
    public static final OfInt receive_bandwidthLAYOUT() {
        return receive_bandwidthLAYOUT;
    }

    private static final long receive_bandwidth$OFFSET = 104;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int receive_bandwidth
     * }
     */
    public static final long receive_bandwidth$offset() {
        return receive_bandwidth$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int receive_bandwidth
     * }
     */
    public static int receive_bandwidth(MemorySegment struct) {
        return struct.get(receive_bandwidthLAYOUT, receive_bandwidth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int receive_bandwidth
     * }
     */
    public static void receive_bandwidth(MemorySegment struct, int fieldValue) {
        struct.set(receive_bandwidthLAYOUT, receive_bandwidth$OFFSET, fieldValue);
    }

    private static final OfInt receive_gainLAYOUT = (OfInt)LAYOUT.select(groupElement("receive_gain"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int receive_gain
     * }
     */
    public static final OfInt receive_gainLAYOUT() {
        return receive_gainLAYOUT;
    }

    private static final long receive_gain$OFFSET = 108;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int receive_gain
     * }
     */
    public static final long receive_gain$offset() {
        return receive_gain$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int receive_gain
     * }
     */
    public static int receive_gain(MemorySegment struct) {
        return struct.get(receive_gainLAYOUT, receive_gain$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int receive_gain
     * }
     */
    public static void receive_gain(MemorySegment struct, int fieldValue) {
        struct.set(receive_gainLAYOUT, receive_gain$OFFSET, fieldValue);
    }

    private static final OfInt cross_over_angleLAYOUT = (OfInt)LAYOUT.select(groupElement("cross_over_angle"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int cross_over_angle
     * }
     */
    public static final OfInt cross_over_angleLAYOUT() {
        return cross_over_angleLAYOUT;
    }

    private static final long cross_over_angle$OFFSET = 112;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int cross_over_angle
     * }
     */
    public static final long cross_over_angle$offset() {
        return cross_over_angle$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int cross_over_angle
     * }
     */
    public static int cross_over_angle(MemorySegment struct) {
        return struct.get(cross_over_angleLAYOUT, cross_over_angle$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int cross_over_angle
     * }
     */
    public static void cross_over_angle(MemorySegment struct, int fieldValue) {
        struct.set(cross_over_angleLAYOUT, cross_over_angle$OFFSET, fieldValue);
    }

    private static final OfInt ssv_sourceLAYOUT = (OfInt)LAYOUT.select(groupElement("ssv_source"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int ssv_source
     * }
     */
    public static final OfInt ssv_sourceLAYOUT() {
        return ssv_sourceLAYOUT;
    }

    private static final long ssv_source$OFFSET = 116;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int ssv_source
     * }
     */
    public static final long ssv_source$offset() {
        return ssv_source$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ssv_source
     * }
     */
    public static int ssv_source(MemorySegment struct) {
        return struct.get(ssv_sourceLAYOUT, ssv_source$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ssv_source
     * }
     */
    public static void ssv_source(MemorySegment struct, int fieldValue) {
        struct.set(ssv_sourceLAYOUT, ssv_source$OFFSET, fieldValue);
    }

    private static final OfInt swath_widthLAYOUT = (OfInt)LAYOUT.select(groupElement("swath_width"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int swath_width
     * }
     */
    public static final OfInt swath_widthLAYOUT() {
        return swath_widthLAYOUT;
    }

    private static final long swath_width$OFFSET = 120;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int swath_width
     * }
     */
    public static final long swath_width$offset() {
        return swath_width$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int swath_width
     * }
     */
    public static int swath_width(MemorySegment struct) {
        return struct.get(swath_widthLAYOUT, swath_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int swath_width
     * }
     */
    public static void swath_width(MemorySegment struct, int fieldValue) {
        struct.set(swath_widthLAYOUT, swath_width$OFFSET, fieldValue);
    }

    private static final OfInt beam_spacingLAYOUT = (OfInt)LAYOUT.select(groupElement("beam_spacing"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int beam_spacing
     * }
     */
    public static final OfInt beam_spacingLAYOUT() {
        return beam_spacingLAYOUT;
    }

    private static final long beam_spacing$OFFSET = 124;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int beam_spacing
     * }
     */
    public static final long beam_spacing$offset() {
        return beam_spacing$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int beam_spacing
     * }
     */
    public static int beam_spacing(MemorySegment struct) {
        return struct.get(beam_spacingLAYOUT, beam_spacing$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int beam_spacing
     * }
     */
    public static void beam_spacing(MemorySegment struct, int fieldValue) {
        struct.set(beam_spacingLAYOUT, beam_spacing$OFFSET, fieldValue);
    }

    private static final OfInt coverage_sectorLAYOUT = (OfInt)LAYOUT.select(groupElement("coverage_sector"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int coverage_sector
     * }
     */
    public static final OfInt coverage_sectorLAYOUT() {
        return coverage_sectorLAYOUT;
    }

    private static final long coverage_sector$OFFSET = 128;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int coverage_sector
     * }
     */
    public static final long coverage_sector$offset() {
        return coverage_sector$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int coverage_sector
     * }
     */
    public static int coverage_sector(MemorySegment struct) {
        return struct.get(coverage_sectorLAYOUT, coverage_sector$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int coverage_sector
     * }
     */
    public static void coverage_sector(MemorySegment struct, int fieldValue) {
        struct.set(coverage_sectorLAYOUT, coverage_sector$OFFSET, fieldValue);
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

    private static final long stabilization$OFFSET = 132;

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

    private static final OfInt port_swath_widthLAYOUT = (OfInt)LAYOUT.select(groupElement("port_swath_width"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int port_swath_width
     * }
     */
    public static final OfInt port_swath_widthLAYOUT() {
        return port_swath_widthLAYOUT;
    }

    private static final long port_swath_width$OFFSET = 136;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int port_swath_width
     * }
     */
    public static final long port_swath_width$offset() {
        return port_swath_width$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int port_swath_width
     * }
     */
    public static int port_swath_width(MemorySegment struct) {
        return struct.get(port_swath_widthLAYOUT, port_swath_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int port_swath_width
     * }
     */
    public static void port_swath_width(MemorySegment struct, int fieldValue) {
        struct.set(port_swath_widthLAYOUT, port_swath_width$OFFSET, fieldValue);
    }

    private static final OfInt stbd_swath_widthLAYOUT = (OfInt)LAYOUT.select(groupElement("stbd_swath_width"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int stbd_swath_width
     * }
     */
    public static final OfInt stbd_swath_widthLAYOUT() {
        return stbd_swath_widthLAYOUT;
    }

    private static final long stbd_swath_width$OFFSET = 140;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int stbd_swath_width
     * }
     */
    public static final long stbd_swath_width$offset() {
        return stbd_swath_width$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int stbd_swath_width
     * }
     */
    public static int stbd_swath_width(MemorySegment struct) {
        return struct.get(stbd_swath_widthLAYOUT, stbd_swath_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int stbd_swath_width
     * }
     */
    public static void stbd_swath_width(MemorySegment struct, int fieldValue) {
        struct.set(stbd_swath_widthLAYOUT, stbd_swath_width$OFFSET, fieldValue);
    }

    private static final OfInt port_coverage_sectorLAYOUT = (OfInt)LAYOUT.select(groupElement("port_coverage_sector"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int port_coverage_sector
     * }
     */
    public static final OfInt port_coverage_sectorLAYOUT() {
        return port_coverage_sectorLAYOUT;
    }

    private static final long port_coverage_sector$OFFSET = 144;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int port_coverage_sector
     * }
     */
    public static final long port_coverage_sector$offset() {
        return port_coverage_sector$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int port_coverage_sector
     * }
     */
    public static int port_coverage_sector(MemorySegment struct) {
        return struct.get(port_coverage_sectorLAYOUT, port_coverage_sector$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int port_coverage_sector
     * }
     */
    public static void port_coverage_sector(MemorySegment struct, int fieldValue) {
        struct.set(port_coverage_sectorLAYOUT, port_coverage_sector$OFFSET, fieldValue);
    }

    private static final OfInt stbd_coverage_sectorLAYOUT = (OfInt)LAYOUT.select(groupElement("stbd_coverage_sector"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int stbd_coverage_sector
     * }
     */
    public static final OfInt stbd_coverage_sectorLAYOUT() {
        return stbd_coverage_sectorLAYOUT;
    }

    private static final long stbd_coverage_sector$OFFSET = 148;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int stbd_coverage_sector
     * }
     */
    public static final long stbd_coverage_sector$offset() {
        return stbd_coverage_sector$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int stbd_coverage_sector
     * }
     */
    public static int stbd_coverage_sector(MemorySegment struct) {
        return struct.get(stbd_coverage_sectorLAYOUT, stbd_coverage_sector$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int stbd_coverage_sector
     * }
     */
    public static void stbd_coverage_sector(MemorySegment struct, int fieldValue) {
        struct.set(stbd_coverage_sectorLAYOUT, stbd_coverage_sector$OFFSET, fieldValue);
    }

    private static final OfInt hilo_freq_absorp_ratioLAYOUT = (OfInt)LAYOUT.select(groupElement("hilo_freq_absorp_ratio"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int hilo_freq_absorp_ratio
     * }
     */
    public static final OfInt hilo_freq_absorp_ratioLAYOUT() {
        return hilo_freq_absorp_ratioLAYOUT;
    }

    private static final long hilo_freq_absorp_ratio$OFFSET = 152;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int hilo_freq_absorp_ratio
     * }
     */
    public static final long hilo_freq_absorp_ratio$offset() {
        return hilo_freq_absorp_ratio$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int hilo_freq_absorp_ratio
     * }
     */
    public static int hilo_freq_absorp_ratio(MemorySegment struct) {
        return struct.get(hilo_freq_absorp_ratioLAYOUT, hilo_freq_absorp_ratio$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int hilo_freq_absorp_ratio
     * }
     */
    public static void hilo_freq_absorp_ratio(MemorySegment struct, int fieldValue) {
        struct.set(hilo_freq_absorp_ratioLAYOUT, hilo_freq_absorp_ratio$OFFSET, fieldValue);
    }

    private static final OfInt spare1LAYOUT = (OfInt)LAYOUT.select(groupElement("spare1"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int spare1
     * }
     */
    public static final OfInt spare1LAYOUT() {
        return spare1LAYOUT;
    }

    private static final long spare1$OFFSET = 156;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int spare1
     * }
     */
    public static final long spare1$offset() {
        return spare1$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int spare1
     * }
     */
    public static int spare1(MemorySegment struct) {
        return struct.get(spare1LAYOUT, spare1$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int spare1
     * }
     */
    public static void spare1(MemorySegment struct, int fieldValue) {
        struct.set(spare1LAYOUT, spare1$OFFSET, fieldValue);
    }

}

