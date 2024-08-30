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
public final class EMRunTime extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("model_number"),
        MemoryLayout.paddingLayout(4),
        TimeSpec.LAYOUT.withName("dg_time"),
        GSF.C_INT.withName("ping_counter"),
        GSF.C_INT.withName("serial_number"),
        GSF.C_CHAR.withName("operator_station_status"),
        GSF.C_CHAR.withName("processing_unit_status"),
        GSF.C_CHAR.withName("bsp_status"),
        GSF.C_CHAR.withName("head_transceiver_status"),
        GSF.C_CHAR.withName("mode"),
        GSF.C_CHAR.withName("filter_id"),
        MemoryLayout.paddingLayout(2),
        GSF.C_DOUBLE.withName("min_depth"),
        GSF.C_DOUBLE.withName("max_depth"),
        GSF.C_DOUBLE.withName("absorption"),
        GSF.C_DOUBLE.withName("tx_pulse_length"),
        GSF.C_DOUBLE.withName("tx_beam_width"),
        GSF.C_DOUBLE.withName("tx_power_re_max"),
        GSF.C_DOUBLE.withName("rx_beam_width"),
        GSF.C_DOUBLE.withName("rx_bandwidth"),
        GSF.C_DOUBLE.withName("rx_fixed_gain"),
        GSF.C_DOUBLE.withName("tvg_cross_over_angle"),
        GSF.C_CHAR.withName("ssv_source"),
        MemoryLayout.paddingLayout(3),
        GSF.C_INT.withName("max_port_swath_width"),
        GSF.C_CHAR.withName("beam_spacing"),
        MemoryLayout.paddingLayout(3),
        GSF.C_INT.withName("max_port_coverage"),
        GSF.C_CHAR.withName("stabilization"),
        MemoryLayout.paddingLayout(3),
        GSF.C_INT.withName("max_stbd_coverage"),
        GSF.C_INT.withName("max_stbd_swath_width"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("durotong_speed"),
        GSF.C_DOUBLE.withName("hi_low_absorption_ratio"),
        GSF.C_DOUBLE.withName("tx_along_tilt"),
        GSF.C_CHAR.withName("filter_id_2"),
        MemoryLayout.sequenceLayout(16, GSF.C_CHAR).withName("spare"),
        MemoryLayout.paddingLayout(7)
    ).withName("t_gsfEMRunTime");

    public EMRunTime(MemorySegment struct) {
        super(struct);
    }

    public EMRunTime(SegmentAllocator allocator) {
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

    private static final OfInt ping_counterLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_counter"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int ping_counter
     * }
     */
    public static final OfInt ping_counterLAYOUT() {
        return ping_counterLAYOUT;
    }

    private static final long ping_counter$OFFSET = 24;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int ping_counter
     * }
     */
    public static final long ping_counter$offset() {
        return ping_counter$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_counter
     * }
     */
    public static int ping_counter(MemorySegment struct) {
        return struct.get(ping_counterLAYOUT, ping_counter$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_counter
     * }
     */
    public static void ping_counter(MemorySegment struct, int fieldValue) {
        struct.set(ping_counterLAYOUT, ping_counter$OFFSET, fieldValue);
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

    private static final OfByte operator_station_statusLAYOUT = (OfByte)LAYOUT.select(groupElement("operator_station_status"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char operator_station_status
     * }
     */
    public static final OfByte operator_station_statusLAYOUT() {
        return operator_station_statusLAYOUT;
    }

    private static final long operator_station_status$OFFSET = 32;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char operator_station_status
     * }
     */
    public static final long operator_station_status$offset() {
        return operator_station_status$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char operator_station_status
     * }
     */
    public static byte operator_station_status(MemorySegment struct) {
        return struct.get(operator_station_statusLAYOUT, operator_station_status$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char operator_station_status
     * }
     */
    public static void operator_station_status(MemorySegment struct, byte fieldValue) {
        struct.set(operator_station_statusLAYOUT, operator_station_status$OFFSET, fieldValue);
    }

    private static final OfByte processing_unit_statusLAYOUT = (OfByte)LAYOUT.select(groupElement("processing_unit_status"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char processing_unit_status
     * }
     */
    public static final OfByte processing_unit_statusLAYOUT() {
        return processing_unit_statusLAYOUT;
    }

    private static final long processing_unit_status$OFFSET = 33;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char processing_unit_status
     * }
     */
    public static final long processing_unit_status$offset() {
        return processing_unit_status$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char processing_unit_status
     * }
     */
    public static byte processing_unit_status(MemorySegment struct) {
        return struct.get(processing_unit_statusLAYOUT, processing_unit_status$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char processing_unit_status
     * }
     */
    public static void processing_unit_status(MemorySegment struct, byte fieldValue) {
        struct.set(processing_unit_statusLAYOUT, processing_unit_status$OFFSET, fieldValue);
    }

    private static final OfByte bsp_statusLAYOUT = (OfByte)LAYOUT.select(groupElement("bsp_status"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char bsp_status
     * }
     */
    public static final OfByte bsp_statusLAYOUT() {
        return bsp_statusLAYOUT;
    }

    private static final long bsp_status$OFFSET = 34;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char bsp_status
     * }
     */
    public static final long bsp_status$offset() {
        return bsp_status$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char bsp_status
     * }
     */
    public static byte bsp_status(MemorySegment struct) {
        return struct.get(bsp_statusLAYOUT, bsp_status$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char bsp_status
     * }
     */
    public static void bsp_status(MemorySegment struct, byte fieldValue) {
        struct.set(bsp_statusLAYOUT, bsp_status$OFFSET, fieldValue);
    }

    private static final OfByte head_transceiver_statusLAYOUT = (OfByte)LAYOUT.select(groupElement("head_transceiver_status"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char head_transceiver_status
     * }
     */
    public static final OfByte head_transceiver_statusLAYOUT() {
        return head_transceiver_statusLAYOUT;
    }

    private static final long head_transceiver_status$OFFSET = 35;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char head_transceiver_status
     * }
     */
    public static final long head_transceiver_status$offset() {
        return head_transceiver_status$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char head_transceiver_status
     * }
     */
    public static byte head_transceiver_status(MemorySegment struct) {
        return struct.get(head_transceiver_statusLAYOUT, head_transceiver_status$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char head_transceiver_status
     * }
     */
    public static void head_transceiver_status(MemorySegment struct, byte fieldValue) {
        struct.set(head_transceiver_statusLAYOUT, head_transceiver_status$OFFSET, fieldValue);
    }

    private static final OfByte modeLAYOUT = (OfByte)LAYOUT.select(groupElement("mode"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char mode
     * }
     */
    public static final OfByte modeLAYOUT() {
        return modeLAYOUT;
    }

    private static final long mode$OFFSET = 36;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char mode
     * }
     */
    public static final long mode$offset() {
        return mode$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char mode
     * }
     */
    public static byte mode(MemorySegment struct) {
        return struct.get(modeLAYOUT, mode$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char mode
     * }
     */
    public static void mode(MemorySegment struct, byte fieldValue) {
        struct.set(modeLAYOUT, mode$OFFSET, fieldValue);
    }

    private static final OfByte filter_idLAYOUT = (OfByte)LAYOUT.select(groupElement("filter_id"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char filter_id
     * }
     */
    public static final OfByte filter_idLAYOUT() {
        return filter_idLAYOUT;
    }

    private static final long filter_id$OFFSET = 37;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char filter_id
     * }
     */
    public static final long filter_id$offset() {
        return filter_id$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char filter_id
     * }
     */
    public static byte filter_id(MemorySegment struct) {
        return struct.get(filter_idLAYOUT, filter_id$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char filter_id
     * }
     */
    public static void filter_id(MemorySegment struct, byte fieldValue) {
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

    private static final long min_depth$OFFSET = 40;

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

    private static final long max_depth$OFFSET = 48;

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

    private static final long absorption$OFFSET = 56;

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

    private static final OfDouble tx_pulse_lengthLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_pulse_length"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double tx_pulse_length
     * }
     */
    public static final OfDouble tx_pulse_lengthLAYOUT() {
        return tx_pulse_lengthLAYOUT;
    }

    private static final long tx_pulse_length$OFFSET = 64;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double tx_pulse_length
     * }
     */
    public static final long tx_pulse_length$offset() {
        return tx_pulse_length$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_pulse_length
     * }
     */
    public static double tx_pulse_length(MemorySegment struct) {
        return struct.get(tx_pulse_lengthLAYOUT, tx_pulse_length$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_pulse_length
     * }
     */
    public static void tx_pulse_length(MemorySegment struct, double fieldValue) {
        struct.set(tx_pulse_lengthLAYOUT, tx_pulse_length$OFFSET, fieldValue);
    }

    private static final OfDouble tx_beam_widthLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_beam_width"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double tx_beam_width
     * }
     */
    public static final OfDouble tx_beam_widthLAYOUT() {
        return tx_beam_widthLAYOUT;
    }

    private static final long tx_beam_width$OFFSET = 72;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double tx_beam_width
     * }
     */
    public static final long tx_beam_width$offset() {
        return tx_beam_width$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_beam_width
     * }
     */
    public static double tx_beam_width(MemorySegment struct) {
        return struct.get(tx_beam_widthLAYOUT, tx_beam_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_beam_width
     * }
     */
    public static void tx_beam_width(MemorySegment struct, double fieldValue) {
        struct.set(tx_beam_widthLAYOUT, tx_beam_width$OFFSET, fieldValue);
    }

    private static final OfDouble tx_power_re_maxLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_power_re_max"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double tx_power_re_max
     * }
     */
    public static final OfDouble tx_power_re_maxLAYOUT() {
        return tx_power_re_maxLAYOUT;
    }

    private static final long tx_power_re_max$OFFSET = 80;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double tx_power_re_max
     * }
     */
    public static final long tx_power_re_max$offset() {
        return tx_power_re_max$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_power_re_max
     * }
     */
    public static double tx_power_re_max(MemorySegment struct) {
        return struct.get(tx_power_re_maxLAYOUT, tx_power_re_max$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_power_re_max
     * }
     */
    public static void tx_power_re_max(MemorySegment struct, double fieldValue) {
        struct.set(tx_power_re_maxLAYOUT, tx_power_re_max$OFFSET, fieldValue);
    }

    private static final OfDouble rx_beam_widthLAYOUT = (OfDouble)LAYOUT.select(groupElement("rx_beam_width"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double rx_beam_width
     * }
     */
    public static final OfDouble rx_beam_widthLAYOUT() {
        return rx_beam_widthLAYOUT;
    }

    private static final long rx_beam_width$OFFSET = 88;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double rx_beam_width
     * }
     */
    public static final long rx_beam_width$offset() {
        return rx_beam_width$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double rx_beam_width
     * }
     */
    public static double rx_beam_width(MemorySegment struct) {
        return struct.get(rx_beam_widthLAYOUT, rx_beam_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double rx_beam_width
     * }
     */
    public static void rx_beam_width(MemorySegment struct, double fieldValue) {
        struct.set(rx_beam_widthLAYOUT, rx_beam_width$OFFSET, fieldValue);
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

    private static final long rx_bandwidth$OFFSET = 96;

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

    private static final OfDouble rx_fixed_gainLAYOUT = (OfDouble)LAYOUT.select(groupElement("rx_fixed_gain"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double rx_fixed_gain
     * }
     */
    public static final OfDouble rx_fixed_gainLAYOUT() {
        return rx_fixed_gainLAYOUT;
    }

    private static final long rx_fixed_gain$OFFSET = 104;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double rx_fixed_gain
     * }
     */
    public static final long rx_fixed_gain$offset() {
        return rx_fixed_gain$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double rx_fixed_gain
     * }
     */
    public static double rx_fixed_gain(MemorySegment struct) {
        return struct.get(rx_fixed_gainLAYOUT, rx_fixed_gain$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double rx_fixed_gain
     * }
     */
    public static void rx_fixed_gain(MemorySegment struct, double fieldValue) {
        struct.set(rx_fixed_gainLAYOUT, rx_fixed_gain$OFFSET, fieldValue);
    }

    private static final OfDouble tvg_cross_over_angleLAYOUT = (OfDouble)LAYOUT.select(groupElement("tvg_cross_over_angle"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double tvg_cross_over_angle
     * }
     */
    public static final OfDouble tvg_cross_over_angleLAYOUT() {
        return tvg_cross_over_angleLAYOUT;
    }

    private static final long tvg_cross_over_angle$OFFSET = 112;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double tvg_cross_over_angle
     * }
     */
    public static final long tvg_cross_over_angle$offset() {
        return tvg_cross_over_angle$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tvg_cross_over_angle
     * }
     */
    public static double tvg_cross_over_angle(MemorySegment struct) {
        return struct.get(tvg_cross_over_angleLAYOUT, tvg_cross_over_angle$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tvg_cross_over_angle
     * }
     */
    public static void tvg_cross_over_angle(MemorySegment struct, double fieldValue) {
        struct.set(tvg_cross_over_angleLAYOUT, tvg_cross_over_angle$OFFSET, fieldValue);
    }

    private static final OfByte ssv_sourceLAYOUT = (OfByte)LAYOUT.select(groupElement("ssv_source"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char ssv_source
     * }
     */
    public static final OfByte ssv_sourceLAYOUT() {
        return ssv_sourceLAYOUT;
    }

    private static final long ssv_source$OFFSET = 120;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char ssv_source
     * }
     */
    public static final long ssv_source$offset() {
        return ssv_source$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char ssv_source
     * }
     */
    public static byte ssv_source(MemorySegment struct) {
        return struct.get(ssv_sourceLAYOUT, ssv_source$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char ssv_source
     * }
     */
    public static void ssv_source(MemorySegment struct, byte fieldValue) {
        struct.set(ssv_sourceLAYOUT, ssv_source$OFFSET, fieldValue);
    }

    private static final OfInt max_port_swath_widthLAYOUT = (OfInt)LAYOUT.select(groupElement("max_port_swath_width"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int max_port_swath_width
     * }
     */
    public static final OfInt max_port_swath_widthLAYOUT() {
        return max_port_swath_widthLAYOUT;
    }

    private static final long max_port_swath_width$OFFSET = 124;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int max_port_swath_width
     * }
     */
    public static final long max_port_swath_width$offset() {
        return max_port_swath_width$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int max_port_swath_width
     * }
     */
    public static int max_port_swath_width(MemorySegment struct) {
        return struct.get(max_port_swath_widthLAYOUT, max_port_swath_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int max_port_swath_width
     * }
     */
    public static void max_port_swath_width(MemorySegment struct, int fieldValue) {
        struct.set(max_port_swath_widthLAYOUT, max_port_swath_width$OFFSET, fieldValue);
    }

    private static final OfByte beam_spacingLAYOUT = (OfByte)LAYOUT.select(groupElement("beam_spacing"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char beam_spacing
     * }
     */
    public static final OfByte beam_spacingLAYOUT() {
        return beam_spacingLAYOUT;
    }

    private static final long beam_spacing$OFFSET = 128;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char beam_spacing
     * }
     */
    public static final long beam_spacing$offset() {
        return beam_spacing$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char beam_spacing
     * }
     */
    public static byte beam_spacing(MemorySegment struct) {
        return struct.get(beam_spacingLAYOUT, beam_spacing$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char beam_spacing
     * }
     */
    public static void beam_spacing(MemorySegment struct, byte fieldValue) {
        struct.set(beam_spacingLAYOUT, beam_spacing$OFFSET, fieldValue);
    }

    private static final OfInt max_port_coverageLAYOUT = (OfInt)LAYOUT.select(groupElement("max_port_coverage"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int max_port_coverage
     * }
     */
    public static final OfInt max_port_coverageLAYOUT() {
        return max_port_coverageLAYOUT;
    }

    private static final long max_port_coverage$OFFSET = 132;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int max_port_coverage
     * }
     */
    public static final long max_port_coverage$offset() {
        return max_port_coverage$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int max_port_coverage
     * }
     */
    public static int max_port_coverage(MemorySegment struct) {
        return struct.get(max_port_coverageLAYOUT, max_port_coverage$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int max_port_coverage
     * }
     */
    public static void max_port_coverage(MemorySegment struct, int fieldValue) {
        struct.set(max_port_coverageLAYOUT, max_port_coverage$OFFSET, fieldValue);
    }

    private static final OfByte stabilizationLAYOUT = (OfByte)LAYOUT.select(groupElement("stabilization"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char stabilization
     * }
     */
    public static final OfByte stabilizationLAYOUT() {
        return stabilizationLAYOUT;
    }

    private static final long stabilization$OFFSET = 136;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char stabilization
     * }
     */
    public static final long stabilization$offset() {
        return stabilization$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char stabilization
     * }
     */
    public static byte stabilization(MemorySegment struct) {
        return struct.get(stabilizationLAYOUT, stabilization$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char stabilization
     * }
     */
    public static void stabilization(MemorySegment struct, byte fieldValue) {
        struct.set(stabilizationLAYOUT, stabilization$OFFSET, fieldValue);
    }

    private static final OfInt max_stbd_coverageLAYOUT = (OfInt)LAYOUT.select(groupElement("max_stbd_coverage"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int max_stbd_coverage
     * }
     */
    public static final OfInt max_stbd_coverageLAYOUT() {
        return max_stbd_coverageLAYOUT;
    }

    private static final long max_stbd_coverage$OFFSET = 140;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int max_stbd_coverage
     * }
     */
    public static final long max_stbd_coverage$offset() {
        return max_stbd_coverage$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int max_stbd_coverage
     * }
     */
    public static int max_stbd_coverage(MemorySegment struct) {
        return struct.get(max_stbd_coverageLAYOUT, max_stbd_coverage$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int max_stbd_coverage
     * }
     */
    public static void max_stbd_coverage(MemorySegment struct, int fieldValue) {
        struct.set(max_stbd_coverageLAYOUT, max_stbd_coverage$OFFSET, fieldValue);
    }

    private static final OfInt max_stbd_swath_widthLAYOUT = (OfInt)LAYOUT.select(groupElement("max_stbd_swath_width"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int max_stbd_swath_width
     * }
     */
    public static final OfInt max_stbd_swath_widthLAYOUT() {
        return max_stbd_swath_widthLAYOUT;
    }

    private static final long max_stbd_swath_width$OFFSET = 144;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int max_stbd_swath_width
     * }
     */
    public static final long max_stbd_swath_width$offset() {
        return max_stbd_swath_width$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int max_stbd_swath_width
     * }
     */
    public static int max_stbd_swath_width(MemorySegment struct) {
        return struct.get(max_stbd_swath_widthLAYOUT, max_stbd_swath_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int max_stbd_swath_width
     * }
     */
    public static void max_stbd_swath_width(MemorySegment struct, int fieldValue) {
        struct.set(max_stbd_swath_widthLAYOUT, max_stbd_swath_width$OFFSET, fieldValue);
    }

    private static final OfDouble durotong_speedLAYOUT = (OfDouble)LAYOUT.select(groupElement("durotong_speed"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double durotong_speed
     * }
     */
    public static final OfDouble durotong_speedLAYOUT() {
        return durotong_speedLAYOUT;
    }

    private static final long durotong_speed$OFFSET = 152;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double durotong_speed
     * }
     */
    public static final long durotong_speed$offset() {
        return durotong_speed$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double durotong_speed
     * }
     */
    public static double durotong_speed(MemorySegment struct) {
        return struct.get(durotong_speedLAYOUT, durotong_speed$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double durotong_speed
     * }
     */
    public static void durotong_speed(MemorySegment struct, double fieldValue) {
        struct.set(durotong_speedLAYOUT, durotong_speed$OFFSET, fieldValue);
    }

    private static final OfDouble hi_low_absorption_ratioLAYOUT = (OfDouble)LAYOUT.select(groupElement("hi_low_absorption_ratio"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double hi_low_absorption_ratio
     * }
     */
    public static final OfDouble hi_low_absorption_ratioLAYOUT() {
        return hi_low_absorption_ratioLAYOUT;
    }

    private static final long hi_low_absorption_ratio$OFFSET = 160;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double hi_low_absorption_ratio
     * }
     */
    public static final long hi_low_absorption_ratio$offset() {
        return hi_low_absorption_ratio$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double hi_low_absorption_ratio
     * }
     */
    public static double hi_low_absorption_ratio(MemorySegment struct) {
        return struct.get(hi_low_absorption_ratioLAYOUT, hi_low_absorption_ratio$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double hi_low_absorption_ratio
     * }
     */
    public static void hi_low_absorption_ratio(MemorySegment struct, double fieldValue) {
        struct.set(hi_low_absorption_ratioLAYOUT, hi_low_absorption_ratio$OFFSET, fieldValue);
    }

    private static final OfDouble tx_along_tiltLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_along_tilt"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double tx_along_tilt
     * }
     */
    public static final OfDouble tx_along_tiltLAYOUT() {
        return tx_along_tiltLAYOUT;
    }

    private static final long tx_along_tilt$OFFSET = 168;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double tx_along_tilt
     * }
     */
    public static final long tx_along_tilt$offset() {
        return tx_along_tilt$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_along_tilt
     * }
     */
    public static double tx_along_tilt(MemorySegment struct) {
        return struct.get(tx_along_tiltLAYOUT, tx_along_tilt$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_along_tilt
     * }
     */
    public static void tx_along_tilt(MemorySegment struct, double fieldValue) {
        struct.set(tx_along_tiltLAYOUT, tx_along_tilt$OFFSET, fieldValue);
    }

    private static final OfByte filter_id_2LAYOUT = (OfByte)LAYOUT.select(groupElement("filter_id_2"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char filter_id_2
     * }
     */
    public static final OfByte filter_id_2LAYOUT() {
        return filter_id_2LAYOUT;
    }

    private static final long filter_id_2$OFFSET = 176;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char filter_id_2
     * }
     */
    public static final long filter_id_2$offset() {
        return filter_id_2$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char filter_id_2
     * }
     */
    public static byte filter_id_2(MemorySegment struct) {
        return struct.get(filter_id_2LAYOUT, filter_id_2$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char filter_id_2
     * }
     */
    public static void filter_id_2(MemorySegment struct, byte fieldValue) {
        struct.set(filter_id_2LAYOUT, filter_id_2$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public static final SequenceLayout spareLAYOUT() {
        return spareLAYOUT;
    }

    private static final long spare$OFFSET = 177;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public static final long spare$offset() {
        return spare$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public static MemorySegment spare(MemorySegment struct) {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public static void spare(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static long[] spare$DIMS = { 16 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public static long[] spare$dimensions() {
        return spare$DIMS;
    }
    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public static byte spare(MemorySegment struct, long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public static void spare(MemorySegment struct, long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

}

