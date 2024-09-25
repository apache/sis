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

    private static final OfInt ping_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_number"));

    private static final long ping_number$OFFSET = 24;

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

    private static final OfInt system_statusLAYOUT = (OfInt)LAYOUT.select(groupElement("system_status"));

    private static final long system_status$OFFSET = 32;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int system_status
     * }
     */
    public int system_status() {
        return struct.get(system_statusLAYOUT, system_status$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int system_status
     * }
     */
    public void system_status(int fieldValue) {
        struct.set(system_statusLAYOUT, system_status$OFFSET, fieldValue);
    }

    private static final OfInt modeLAYOUT = (OfInt)LAYOUT.select(groupElement("mode"));

    private static final long mode$OFFSET = 36;

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

    private static final OfInt filter_idLAYOUT = (OfInt)LAYOUT.select(groupElement("filter_id"));

    private static final long filter_id$OFFSET = 40;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int filter_id
     * }
     */
    public int filter_id() {
        return struct.get(filter_idLAYOUT, filter_id$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int filter_id
     * }
     */
    public void filter_id(int fieldValue) {
        struct.set(filter_idLAYOUT, filter_id$OFFSET, fieldValue);
    }

    private static final OfDouble min_depthLAYOUT = (OfDouble)LAYOUT.select(groupElement("min_depth"));

    private static final long min_depth$OFFSET = 48;

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

    private static final long max_depth$OFFSET = 56;

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

    private static final long absorption$OFFSET = 64;

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

    private static final OfDouble pulse_lengthLAYOUT = (OfDouble)LAYOUT.select(groupElement("pulse_length"));

    private static final long pulse_length$OFFSET = 72;

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

    private static final OfDouble transmit_beam_widthLAYOUT = (OfDouble)LAYOUT.select(groupElement("transmit_beam_width"));

    private static final long transmit_beam_width$OFFSET = 80;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double transmit_beam_width
     * }
     */
    public double transmit_beam_width() {
        return struct.get(transmit_beam_widthLAYOUT, transmit_beam_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double transmit_beam_width
     * }
     */
    public void transmit_beam_width(double fieldValue) {
        struct.set(transmit_beam_widthLAYOUT, transmit_beam_width$OFFSET, fieldValue);
    }

    private static final OfInt power_reductionLAYOUT = (OfInt)LAYOUT.select(groupElement("power_reduction"));

    private static final long power_reduction$OFFSET = 88;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int power_reduction
     * }
     */
    public int power_reduction() {
        return struct.get(power_reductionLAYOUT, power_reduction$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int power_reduction
     * }
     */
    public void power_reduction(int fieldValue) {
        struct.set(power_reductionLAYOUT, power_reduction$OFFSET, fieldValue);
    }

    private static final OfDouble receive_beam_widthLAYOUT = (OfDouble)LAYOUT.select(groupElement("receive_beam_width"));

    private static final long receive_beam_width$OFFSET = 96;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double receive_beam_width
     * }
     */
    public double receive_beam_width() {
        return struct.get(receive_beam_widthLAYOUT, receive_beam_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double receive_beam_width
     * }
     */
    public void receive_beam_width(double fieldValue) {
        struct.set(receive_beam_widthLAYOUT, receive_beam_width$OFFSET, fieldValue);
    }

    private static final OfInt receive_bandwidthLAYOUT = (OfInt)LAYOUT.select(groupElement("receive_bandwidth"));

    private static final long receive_bandwidth$OFFSET = 104;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int receive_bandwidth
     * }
     */
    public int receive_bandwidth() {
        return struct.get(receive_bandwidthLAYOUT, receive_bandwidth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int receive_bandwidth
     * }
     */
    public void receive_bandwidth(int fieldValue) {
        struct.set(receive_bandwidthLAYOUT, receive_bandwidth$OFFSET, fieldValue);
    }

    private static final OfInt receive_gainLAYOUT = (OfInt)LAYOUT.select(groupElement("receive_gain"));

    private static final long receive_gain$OFFSET = 108;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int receive_gain
     * }
     */
    public int receive_gain() {
        return struct.get(receive_gainLAYOUT, receive_gain$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int receive_gain
     * }
     */
    public void receive_gain(int fieldValue) {
        struct.set(receive_gainLAYOUT, receive_gain$OFFSET, fieldValue);
    }

    private static final OfInt cross_over_angleLAYOUT = (OfInt)LAYOUT.select(groupElement("cross_over_angle"));

    private static final long cross_over_angle$OFFSET = 112;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int cross_over_angle
     * }
     */
    public int cross_over_angle() {
        return struct.get(cross_over_angleLAYOUT, cross_over_angle$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int cross_over_angle
     * }
     */
    public void cross_over_angle(int fieldValue) {
        struct.set(cross_over_angleLAYOUT, cross_over_angle$OFFSET, fieldValue);
    }

    private static final OfInt ssv_sourceLAYOUT = (OfInt)LAYOUT.select(groupElement("ssv_source"));

    private static final long ssv_source$OFFSET = 116;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ssv_source
     * }
     */
    public int ssv_source() {
        return struct.get(ssv_sourceLAYOUT, ssv_source$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ssv_source
     * }
     */
    public void ssv_source(int fieldValue) {
        struct.set(ssv_sourceLAYOUT, ssv_source$OFFSET, fieldValue);
    }

    private static final OfInt swath_widthLAYOUT = (OfInt)LAYOUT.select(groupElement("swath_width"));

    private static final long swath_width$OFFSET = 120;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int swath_width
     * }
     */
    public int swath_width() {
        return struct.get(swath_widthLAYOUT, swath_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int swath_width
     * }
     */
    public void swath_width(int fieldValue) {
        struct.set(swath_widthLAYOUT, swath_width$OFFSET, fieldValue);
    }

    private static final OfInt beam_spacingLAYOUT = (OfInt)LAYOUT.select(groupElement("beam_spacing"));

    private static final long beam_spacing$OFFSET = 124;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int beam_spacing
     * }
     */
    public int beam_spacing() {
        return struct.get(beam_spacingLAYOUT, beam_spacing$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int beam_spacing
     * }
     */
    public void beam_spacing(int fieldValue) {
        struct.set(beam_spacingLAYOUT, beam_spacing$OFFSET, fieldValue);
    }

    private static final OfInt coverage_sectorLAYOUT = (OfInt)LAYOUT.select(groupElement("coverage_sector"));

    private static final long coverage_sector$OFFSET = 128;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int coverage_sector
     * }
     */
    public int coverage_sector() {
        return struct.get(coverage_sectorLAYOUT, coverage_sector$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int coverage_sector
     * }
     */
    public void coverage_sector(int fieldValue) {
        struct.set(coverage_sectorLAYOUT, coverage_sector$OFFSET, fieldValue);
    }

    private static final OfInt stabilizationLAYOUT = (OfInt)LAYOUT.select(groupElement("stabilization"));

    private static final long stabilization$OFFSET = 132;

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

    private static final OfInt port_swath_widthLAYOUT = (OfInt)LAYOUT.select(groupElement("port_swath_width"));

    private static final long port_swath_width$OFFSET = 136;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int port_swath_width
     * }
     */
    public int port_swath_width() {
        return struct.get(port_swath_widthLAYOUT, port_swath_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int port_swath_width
     * }
     */
    public void port_swath_width(int fieldValue) {
        struct.set(port_swath_widthLAYOUT, port_swath_width$OFFSET, fieldValue);
    }

    private static final OfInt stbd_swath_widthLAYOUT = (OfInt)LAYOUT.select(groupElement("stbd_swath_width"));

    private static final long stbd_swath_width$OFFSET = 140;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int stbd_swath_width
     * }
     */
    public int stbd_swath_width() {
        return struct.get(stbd_swath_widthLAYOUT, stbd_swath_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int stbd_swath_width
     * }
     */
    public void stbd_swath_width(int fieldValue) {
        struct.set(stbd_swath_widthLAYOUT, stbd_swath_width$OFFSET, fieldValue);
    }

    private static final OfInt port_coverage_sectorLAYOUT = (OfInt)LAYOUT.select(groupElement("port_coverage_sector"));

    private static final long port_coverage_sector$OFFSET = 144;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int port_coverage_sector
     * }
     */
    public int port_coverage_sector() {
        return struct.get(port_coverage_sectorLAYOUT, port_coverage_sector$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int port_coverage_sector
     * }
     */
    public void port_coverage_sector(int fieldValue) {
        struct.set(port_coverage_sectorLAYOUT, port_coverage_sector$OFFSET, fieldValue);
    }

    private static final OfInt stbd_coverage_sectorLAYOUT = (OfInt)LAYOUT.select(groupElement("stbd_coverage_sector"));

    private static final long stbd_coverage_sector$OFFSET = 148;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int stbd_coverage_sector
     * }
     */
    public int stbd_coverage_sector() {
        return struct.get(stbd_coverage_sectorLAYOUT, stbd_coverage_sector$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int stbd_coverage_sector
     * }
     */
    public void stbd_coverage_sector(int fieldValue) {
        struct.set(stbd_coverage_sectorLAYOUT, stbd_coverage_sector$OFFSET, fieldValue);
    }

    private static final OfInt hilo_freq_absorp_ratioLAYOUT = (OfInt)LAYOUT.select(groupElement("hilo_freq_absorp_ratio"));

    private static final long hilo_freq_absorp_ratio$OFFSET = 152;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int hilo_freq_absorp_ratio
     * }
     */
    public int hilo_freq_absorp_ratio() {
        return struct.get(hilo_freq_absorp_ratioLAYOUT, hilo_freq_absorp_ratio$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int hilo_freq_absorp_ratio
     * }
     */
    public void hilo_freq_absorp_ratio(int fieldValue) {
        struct.set(hilo_freq_absorp_ratioLAYOUT, hilo_freq_absorp_ratio$OFFSET, fieldValue);
    }

    private static final OfInt spare1LAYOUT = (OfInt)LAYOUT.select(groupElement("spare1"));

    private static final long spare1$OFFSET = 156;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int spare1
     * }
     */
    public int spare1() {
        return struct.get(spare1LAYOUT, spare1$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int spare1
     * }
     */
    public void spare1(int fieldValue) {
        struct.set(spare1LAYOUT, spare1$OFFSET, fieldValue);
    }
}
