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

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt model_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("model_number"));

    private static final long model_number$OFFSET = 0;

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

    private static final GroupLayout dg_timeLAYOUT = (GroupLayout)LAYOUT.select(groupElement("dg_time"));

    private static final long dg_time$OFFSET = 8;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * struct timespec dg_time
     * }
     */
    public MemorySegment dg_time() {
        return struct.asSlice(dg_time$OFFSET, dg_timeLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * struct timespec dg_time
     * }
     */
    public void dg_time(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, dg_time$OFFSET, dg_timeLAYOUT.byteSize());
    }

    private static final OfInt ping_counterLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_counter"));

    private static final long ping_counter$OFFSET = 24;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_counter
     * }
     */
    public int ping_counter() {
        return struct.get(ping_counterLAYOUT, ping_counter$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_counter
     * }
     */
    public void ping_counter(int fieldValue) {
        struct.set(ping_counterLAYOUT, ping_counter$OFFSET, fieldValue);
    }

    private static final OfInt serial_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("serial_number"));

    private static final long serial_number$OFFSET = 28;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int serial_number
     * }
     */
    public int serial_number() {
        return struct.get(serial_numberLAYOUT, serial_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int serial_number
     * }
     */
    public void serial_number(int fieldValue) {
        struct.set(serial_numberLAYOUT, serial_number$OFFSET, fieldValue);
    }

    private static final OfByte operator_station_statusLAYOUT = (OfByte)LAYOUT.select(groupElement("operator_station_status"));

    private static final long operator_station_status$OFFSET = 32;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char operator_station_status
     * }
     */
    public byte operator_station_status() {
        return struct.get(operator_station_statusLAYOUT, operator_station_status$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char operator_station_status
     * }
     */
    public void operator_station_status(byte fieldValue) {
        struct.set(operator_station_statusLAYOUT, operator_station_status$OFFSET, fieldValue);
    }

    private static final OfByte processing_unit_statusLAYOUT = (OfByte)LAYOUT.select(groupElement("processing_unit_status"));

    private static final long processing_unit_status$OFFSET = 33;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char processing_unit_status
     * }
     */
    public byte processing_unit_status() {
        return struct.get(processing_unit_statusLAYOUT, processing_unit_status$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char processing_unit_status
     * }
     */
    public void processing_unit_status(byte fieldValue) {
        struct.set(processing_unit_statusLAYOUT, processing_unit_status$OFFSET, fieldValue);
    }

    private static final OfByte bsp_statusLAYOUT = (OfByte)LAYOUT.select(groupElement("bsp_status"));

    private static final long bsp_status$OFFSET = 34;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char bsp_status
     * }
     */
    public byte bsp_status() {
        return struct.get(bsp_statusLAYOUT, bsp_status$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char bsp_status
     * }
     */
    public void bsp_status(byte fieldValue) {
        struct.set(bsp_statusLAYOUT, bsp_status$OFFSET, fieldValue);
    }

    private static final OfByte head_transceiver_statusLAYOUT = (OfByte)LAYOUT.select(groupElement("head_transceiver_status"));

    private static final long head_transceiver_status$OFFSET = 35;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char head_transceiver_status
     * }
     */
    public byte head_transceiver_status() {
        return struct.get(head_transceiver_statusLAYOUT, head_transceiver_status$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char head_transceiver_status
     * }
     */
    public void head_transceiver_status(byte fieldValue) {
        struct.set(head_transceiver_statusLAYOUT, head_transceiver_status$OFFSET, fieldValue);
    }

    private static final OfByte modeLAYOUT = (OfByte)LAYOUT.select(groupElement("mode"));

    private static final long mode$OFFSET = 36;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char mode
     * }
     */
    public byte mode() {
        return struct.get(modeLAYOUT, mode$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char mode
     * }
     */
    public void mode(byte fieldValue) {
        struct.set(modeLAYOUT, mode$OFFSET, fieldValue);
    }

    private static final OfByte filter_idLAYOUT = (OfByte)LAYOUT.select(groupElement("filter_id"));

    private static final long filter_id$OFFSET = 37;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char filter_id
     * }
     */
    public byte filter_id() {
        return struct.get(filter_idLAYOUT, filter_id$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char filter_id
     * }
     */
    public void filter_id(byte fieldValue) {
        struct.set(filter_idLAYOUT, filter_id$OFFSET, fieldValue);
    }

    private static final OfDouble min_depthLAYOUT = (OfDouble)LAYOUT.select(groupElement("min_depth"));

    private static final long min_depth$OFFSET = 40;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double min_depth
     * }
     */
    public double min_depth() {
        return struct.get(min_depthLAYOUT, min_depth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double min_depth
     * }
     */
    public void min_depth(double fieldValue) {
        struct.set(min_depthLAYOUT, min_depth$OFFSET, fieldValue);
    }

    private static final OfDouble max_depthLAYOUT = (OfDouble)LAYOUT.select(groupElement("max_depth"));

    private static final long max_depth$OFFSET = 48;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double max_depth
     * }
     */
    public double max_depth() {
        return struct.get(max_depthLAYOUT, max_depth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double max_depth
     * }
     */
    public void max_depth(double fieldValue) {
        struct.set(max_depthLAYOUT, max_depth$OFFSET, fieldValue);
    }

    private static final OfDouble absorptionLAYOUT = (OfDouble)LAYOUT.select(groupElement("absorption"));

    private static final long absorption$OFFSET = 56;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double absorption
     * }
     */
    public double absorption() {
        return struct.get(absorptionLAYOUT, absorption$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double absorption
     * }
     */
    public void absorption(double fieldValue) {
        struct.set(absorptionLAYOUT, absorption$OFFSET, fieldValue);
    }

    private static final OfDouble tx_pulse_lengthLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_pulse_length"));

    private static final long tx_pulse_length$OFFSET = 64;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_pulse_length
     * }
     */
    public double tx_pulse_length() {
        return struct.get(tx_pulse_lengthLAYOUT, tx_pulse_length$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_pulse_length
     * }
     */
    public void tx_pulse_length(double fieldValue) {
        struct.set(tx_pulse_lengthLAYOUT, tx_pulse_length$OFFSET, fieldValue);
    }

    private static final OfDouble tx_beam_widthLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_beam_width"));

    private static final long tx_beam_width$OFFSET = 72;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_beam_width
     * }
     */
    public double tx_beam_width() {
        return struct.get(tx_beam_widthLAYOUT, tx_beam_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_beam_width
     * }
     */
    public void tx_beam_width(double fieldValue) {
        struct.set(tx_beam_widthLAYOUT, tx_beam_width$OFFSET, fieldValue);
    }

    private static final OfDouble tx_power_re_maxLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_power_re_max"));

    private static final long tx_power_re_max$OFFSET = 80;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_power_re_max
     * }
     */
    public double tx_power_re_max() {
        return struct.get(tx_power_re_maxLAYOUT, tx_power_re_max$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_power_re_max
     * }
     */
    public void tx_power_re_max(double fieldValue) {
        struct.set(tx_power_re_maxLAYOUT, tx_power_re_max$OFFSET, fieldValue);
    }

    private static final OfDouble rx_beam_widthLAYOUT = (OfDouble)LAYOUT.select(groupElement("rx_beam_width"));

    private static final long rx_beam_width$OFFSET = 88;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double rx_beam_width
     * }
     */
    public double rx_beam_width() {
        return struct.get(rx_beam_widthLAYOUT, rx_beam_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double rx_beam_width
     * }
     */
    public void rx_beam_width(double fieldValue) {
        struct.set(rx_beam_widthLAYOUT, rx_beam_width$OFFSET, fieldValue);
    }

    private static final OfDouble rx_bandwidthLAYOUT = (OfDouble)LAYOUT.select(groupElement("rx_bandwidth"));

    private static final long rx_bandwidth$OFFSET = 96;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double rx_bandwidth
     * }
     */
    public double rx_bandwidth() {
        return struct.get(rx_bandwidthLAYOUT, rx_bandwidth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double rx_bandwidth
     * }
     */
    public void rx_bandwidth(double fieldValue) {
        struct.set(rx_bandwidthLAYOUT, rx_bandwidth$OFFSET, fieldValue);
    }

    private static final OfDouble rx_fixed_gainLAYOUT = (OfDouble)LAYOUT.select(groupElement("rx_fixed_gain"));

    private static final long rx_fixed_gain$OFFSET = 104;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double rx_fixed_gain
     * }
     */
    public double rx_fixed_gain() {
        return struct.get(rx_fixed_gainLAYOUT, rx_fixed_gain$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double rx_fixed_gain
     * }
     */
    public void rx_fixed_gain(double fieldValue) {
        struct.set(rx_fixed_gainLAYOUT, rx_fixed_gain$OFFSET, fieldValue);
    }

    private static final OfDouble tvg_cross_over_angleLAYOUT = (OfDouble)LAYOUT.select(groupElement("tvg_cross_over_angle"));

    private static final long tvg_cross_over_angle$OFFSET = 112;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tvg_cross_over_angle
     * }
     */
    public double tvg_cross_over_angle() {
        return struct.get(tvg_cross_over_angleLAYOUT, tvg_cross_over_angle$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tvg_cross_over_angle
     * }
     */
    public void tvg_cross_over_angle(double fieldValue) {
        struct.set(tvg_cross_over_angleLAYOUT, tvg_cross_over_angle$OFFSET, fieldValue);
    }

    private static final OfByte ssv_sourceLAYOUT = (OfByte)LAYOUT.select(groupElement("ssv_source"));

    private static final long ssv_source$OFFSET = 120;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char ssv_source
     * }
     */
    public byte ssv_source() {
        return struct.get(ssv_sourceLAYOUT, ssv_source$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char ssv_source
     * }
     */
    public void ssv_source(byte fieldValue) {
        struct.set(ssv_sourceLAYOUT, ssv_source$OFFSET, fieldValue);
    }

    private static final OfInt max_port_swath_widthLAYOUT = (OfInt)LAYOUT.select(groupElement("max_port_swath_width"));

    private static final long max_port_swath_width$OFFSET = 124;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int max_port_swath_width
     * }
     */
    public int max_port_swath_width() {
        return struct.get(max_port_swath_widthLAYOUT, max_port_swath_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int max_port_swath_width
     * }
     */
    public void max_port_swath_width(int fieldValue) {
        struct.set(max_port_swath_widthLAYOUT, max_port_swath_width$OFFSET, fieldValue);
    }

    private static final OfByte beam_spacingLAYOUT = (OfByte)LAYOUT.select(groupElement("beam_spacing"));

    private static final long beam_spacing$OFFSET = 128;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char beam_spacing
     * }
     */
    public byte beam_spacing() {
        return struct.get(beam_spacingLAYOUT, beam_spacing$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char beam_spacing
     * }
     */
    public void beam_spacing(byte fieldValue) {
        struct.set(beam_spacingLAYOUT, beam_spacing$OFFSET, fieldValue);
    }

    private static final OfInt max_port_coverageLAYOUT = (OfInt)LAYOUT.select(groupElement("max_port_coverage"));

    private static final long max_port_coverage$OFFSET = 132;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int max_port_coverage
     * }
     */
    public int max_port_coverage() {
        return struct.get(max_port_coverageLAYOUT, max_port_coverage$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int max_port_coverage
     * }
     */
    public void max_port_coverage(int fieldValue) {
        struct.set(max_port_coverageLAYOUT, max_port_coverage$OFFSET, fieldValue);
    }

    private static final OfByte stabilizationLAYOUT = (OfByte)LAYOUT.select(groupElement("stabilization"));

    private static final long stabilization$OFFSET = 136;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char stabilization
     * }
     */
    public byte stabilization() {
        return struct.get(stabilizationLAYOUT, stabilization$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char stabilization
     * }
     */
    public void stabilization(byte fieldValue) {
        struct.set(stabilizationLAYOUT, stabilization$OFFSET, fieldValue);
    }

    private static final OfInt max_stbd_coverageLAYOUT = (OfInt)LAYOUT.select(groupElement("max_stbd_coverage"));

    private static final long max_stbd_coverage$OFFSET = 140;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int max_stbd_coverage
     * }
     */
    public int max_stbd_coverage() {
        return struct.get(max_stbd_coverageLAYOUT, max_stbd_coverage$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int max_stbd_coverage
     * }
     */
    public void max_stbd_coverage(int fieldValue) {
        struct.set(max_stbd_coverageLAYOUT, max_stbd_coverage$OFFSET, fieldValue);
    }

    private static final OfInt max_stbd_swath_widthLAYOUT = (OfInt)LAYOUT.select(groupElement("max_stbd_swath_width"));

    private static final long max_stbd_swath_width$OFFSET = 144;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int max_stbd_swath_width
     * }
     */
    public int max_stbd_swath_width() {
        return struct.get(max_stbd_swath_widthLAYOUT, max_stbd_swath_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int max_stbd_swath_width
     * }
     */
    public void max_stbd_swath_width(int fieldValue) {
        struct.set(max_stbd_swath_widthLAYOUT, max_stbd_swath_width$OFFSET, fieldValue);
    }

    private static final OfDouble durotong_speedLAYOUT = (OfDouble)LAYOUT.select(groupElement("durotong_speed"));

    private static final long durotong_speed$OFFSET = 152;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double durotong_speed
     * }
     */
    public double durotong_speed() {
        return struct.get(durotong_speedLAYOUT, durotong_speed$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double durotong_speed
     * }
     */
    public void durotong_speed(double fieldValue) {
        struct.set(durotong_speedLAYOUT, durotong_speed$OFFSET, fieldValue);
    }

    private static final OfDouble hi_low_absorption_ratioLAYOUT = (OfDouble)LAYOUT.select(groupElement("hi_low_absorption_ratio"));

    private static final long hi_low_absorption_ratio$OFFSET = 160;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double hi_low_absorption_ratio
     * }
     */
    public double hi_low_absorption_ratio() {
        return struct.get(hi_low_absorption_ratioLAYOUT, hi_low_absorption_ratio$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double hi_low_absorption_ratio
     * }
     */
    public void hi_low_absorption_ratio(double fieldValue) {
        struct.set(hi_low_absorption_ratioLAYOUT, hi_low_absorption_ratio$OFFSET, fieldValue);
    }

    private static final OfDouble tx_along_tiltLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_along_tilt"));

    private static final long tx_along_tilt$OFFSET = 168;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_along_tilt
     * }
     */
    public double tx_along_tilt() {
        return struct.get(tx_along_tiltLAYOUT, tx_along_tilt$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_along_tilt
     * }
     */
    public void tx_along_tilt(double fieldValue) {
        struct.set(tx_along_tiltLAYOUT, tx_along_tilt$OFFSET, fieldValue);
    }

    private static final OfByte filter_id_2LAYOUT = (OfByte)LAYOUT.select(groupElement("filter_id_2"));

    private static final long filter_id_2$OFFSET = 176;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char filter_id_2
     * }
     */
    public byte filter_id_2() {
        return struct.get(filter_id_2LAYOUT, filter_id_2$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char filter_id_2
     * }
     */
    public void filter_id_2(byte fieldValue) {
        struct.set(filter_id_2LAYOUT, filter_id_2$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    private static final long spare$OFFSET = 177;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public MemorySegment spare() {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public void spare(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public byte spare(long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public void spare(long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }
}
