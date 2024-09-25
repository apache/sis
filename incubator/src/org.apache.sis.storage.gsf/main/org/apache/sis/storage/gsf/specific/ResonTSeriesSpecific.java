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
public final class ResonTSeriesSpecific extends StructClass {
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("protocol_version"),
        GSF.C_INT.withName("device_id"),
        GSF.C_INT.withName("number_devices"),
        GSF.C_SHORT.withName("system_enumerator"),
        MemoryLayout.sequenceLayout(10, GSF.C_CHAR).withName("reserved_1"),
        GSF.C_INT.withName("major_serial_number"),
        GSF.C_INT.withName("minor_serial_number"),
        GSF.C_INT.withName("ping_number"),
        GSF.C_INT.withName("multi_ping_seq"),
        GSF.C_DOUBLE.withName("frequency"),
        GSF.C_DOUBLE.withName("sample_rate"),
        GSF.C_DOUBLE.withName("receiver_bandwdth"),
        GSF.C_DOUBLE.withName("tx_pulse_width"),
        GSF.C_INT.withName("tx_pulse_type_id"),
        GSF.C_INT.withName("tx_pulse_envlp_id"),
        GSF.C_DOUBLE.withName("tx_pulse_envlp_param"),
        GSF.C_SHORT.withName("tx_pulse_mode"),
        GSF.C_SHORT.withName("tx_pulse_reserved"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("max_ping_rate"),
        GSF.C_DOUBLE.withName("ping_period"),
        GSF.C_DOUBLE.withName("range"),
        GSF.C_DOUBLE.withName("power"),
        GSF.C_DOUBLE.withName("gain"),
        GSF.C_INT.withName("control_flags"),
        GSF.C_INT.withName("projector_id"),
        GSF.C_DOUBLE.withName("projector_steer_angl_vert"),
        GSF.C_DOUBLE.withName("projector_steer_angl_horz"),
        GSF.C_DOUBLE.withName("projector_beam_wdth_vert"),
        GSF.C_DOUBLE.withName("projector_beam_wdth_horz"),
        GSF.C_DOUBLE.withName("projector_beam_focal_pt"),
        GSF.C_INT.withName("projector_beam_weighting_window_type"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("projector_beam_weighting_window_param"),
        GSF.C_INT.withName("transmit_flags"),
        GSF.C_INT.withName("hydrophone_id"),
        GSF.C_INT.withName("receiving_beam_weighting_window_type"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("receiving_beam_weighting_window_param"),
        GSF.C_INT.withName("receive_flags"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("receive_beam_width"),
        GSF.C_DOUBLE.withName("range_filt_min"),
        GSF.C_DOUBLE.withName("range_filt_max"),
        GSF.C_DOUBLE.withName("depth_filt_min"),
        GSF.C_DOUBLE.withName("depth_filt_max"),
        GSF.C_DOUBLE.withName("absorption"),
        GSF.C_DOUBLE.withName("sound_velocity"),
        GSF.C_CHAR.withName("sv_source"),
        MemoryLayout.paddingLayout(7),
        GSF.C_DOUBLE.withName("spreading"),
        GSF.C_SHORT.withName("beam_spacing_mode"),
        GSF.C_SHORT.withName("sonar_source_mode"),
        GSF.C_CHAR.withName("coverage_mode"),
        MemoryLayout.paddingLayout(3),
        GSF.C_DOUBLE.withName("coverage_angle"),
        GSF.C_DOUBLE.withName("horizontal_receiver_steering_angle"),
        MemoryLayout.sequenceLayout(3, GSF.C_CHAR).withName("reserved_2"),
        MemoryLayout.paddingLayout(1),
        GSF.C_INT.withName("uncertainty_type"),
        GSF.C_DOUBLE.withName("transmitter_steering_angle"),
        GSF.C_DOUBLE.withName("applied_roll"),
        GSF.C_SHORT.withName("detection_algorithm"),
        MemoryLayout.paddingLayout(2),
        GSF.C_INT.withName("detection_flags"),
        MemoryLayout.sequenceLayout(60, GSF.C_CHAR).withName("device_description"),
        MemoryLayout.sequenceLayout(420, GSF.C_CHAR).withName("reserved_7027"),
        MemoryLayout.sequenceLayout(32, GSF.C_CHAR).withName("reserved_3")
    ).withName("t_gsfResonTSeriesSpecific");

    public ResonTSeriesSpecific(MemorySegment struct) {
        super(struct);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt protocol_versionLAYOUT = (OfInt)LAYOUT.select(groupElement("protocol_version"));

    private static final long protocol_version$OFFSET = 0;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int protocol_version
     * }
     */
    public int protocol_version() {
        return struct.get(protocol_versionLAYOUT, protocol_version$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int protocol_version
     * }
     */
    public void protocol_version(int fieldValue) {
        struct.set(protocol_versionLAYOUT, protocol_version$OFFSET, fieldValue);
    }

    private static final OfInt device_idLAYOUT = (OfInt)LAYOUT.select(groupElement("device_id"));

    private static final long device_id$OFFSET = 4;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int device_id
     * }
     */
    public int device_id() {
        return struct.get(device_idLAYOUT, device_id$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int device_id
     * }
     */
    public void device_id(int fieldValue) {
        struct.set(device_idLAYOUT, device_id$OFFSET, fieldValue);
    }

    private static final OfInt number_devicesLAYOUT = (OfInt)LAYOUT.select(groupElement("number_devices"));

    private static final long number_devices$OFFSET = 8;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int number_devices
     * }
     */
    public int number_devices() {
        return struct.get(number_devicesLAYOUT, number_devices$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int number_devices
     * }
     */
    public void number_devices(int fieldValue) {
        struct.set(number_devicesLAYOUT, number_devices$OFFSET, fieldValue);
    }

    private static final OfShort system_enumeratorLAYOUT = (OfShort)LAYOUT.select(groupElement("system_enumerator"));

    private static final long system_enumerator$OFFSET = 12;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short system_enumerator
     * }
     */
    public short system_enumerator() {
        return struct.get(system_enumeratorLAYOUT, system_enumerator$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short system_enumerator
     * }
     */
    public void system_enumerator(short fieldValue) {
        struct.set(system_enumeratorLAYOUT, system_enumerator$OFFSET, fieldValue);
    }

    private static final SequenceLayout reserved_1LAYOUT = (SequenceLayout)LAYOUT.select(groupElement("reserved_1"));

    private static final long reserved_1$OFFSET = 14;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char reserved_1[10]
     * }
     */
    public MemorySegment reserved_1() {
        return struct.asSlice(reserved_1$OFFSET, reserved_1LAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char reserved_1[10]
     * }
     */
    public void reserved_1(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, reserved_1$OFFSET, reserved_1LAYOUT.byteSize());
    }

    private static final VarHandle reserved_1$ELEM_HANDLE = reserved_1LAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char reserved_1[10]
     * }
     */
    public byte reserved_1(long index0) {
        return (byte)reserved_1$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char reserved_1[10]
     * }
     */
    public void reserved_1(long index0, byte fieldValue) {
        reserved_1$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final OfInt major_serial_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("major_serial_number"));

    private static final long major_serial_number$OFFSET = 24;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int major_serial_number
     * }
     */
    public int major_serial_number() {
        return struct.get(major_serial_numberLAYOUT, major_serial_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int major_serial_number
     * }
     */
    public void major_serial_number(int fieldValue) {
        struct.set(major_serial_numberLAYOUT, major_serial_number$OFFSET, fieldValue);
    }

    private static final OfInt minor_serial_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("minor_serial_number"));

    private static final long minor_serial_number$OFFSET = 28;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int minor_serial_number
     * }
     */
    public int minor_serial_number() {
        return struct.get(minor_serial_numberLAYOUT, minor_serial_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int minor_serial_number
     * }
     */
    public void minor_serial_number(int fieldValue) {
        struct.set(minor_serial_numberLAYOUT, minor_serial_number$OFFSET, fieldValue);
    }

    private static final OfInt ping_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_number"));

    private static final long ping_number$OFFSET = 32;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int ping_number
     * }
     */
    public int ping_number() {
        return struct.get(ping_numberLAYOUT, ping_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int ping_number
     * }
     */
    public void ping_number(int fieldValue) {
        struct.set(ping_numberLAYOUT, ping_number$OFFSET, fieldValue);
    }

    private static final OfInt multi_ping_seqLAYOUT = (OfInt)LAYOUT.select(groupElement("multi_ping_seq"));

    private static final long multi_ping_seq$OFFSET = 36;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int multi_ping_seq
     * }
     */
    public int multi_ping_seq() {
        return struct.get(multi_ping_seqLAYOUT, multi_ping_seq$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int multi_ping_seq
     * }
     */
    public void multi_ping_seq(int fieldValue) {
        struct.set(multi_ping_seqLAYOUT, multi_ping_seq$OFFSET, fieldValue);
    }

    private static final OfDouble frequencyLAYOUT = (OfDouble)LAYOUT.select(groupElement("frequency"));

    private static final long frequency$OFFSET = 40;

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

    private static final OfDouble sample_rateLAYOUT = (OfDouble)LAYOUT.select(groupElement("sample_rate"));

    private static final long sample_rate$OFFSET = 48;

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

    private static final OfDouble receiver_bandwdthLAYOUT = (OfDouble)LAYOUT.select(groupElement("receiver_bandwdth"));

    private static final long receiver_bandwdth$OFFSET = 56;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double receiver_bandwdth
     * }
     */
    public double receiver_bandwdth() {
        return struct.get(receiver_bandwdthLAYOUT, receiver_bandwdth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double receiver_bandwdth
     * }
     */
    public void receiver_bandwdth(double fieldValue) {
        struct.set(receiver_bandwdthLAYOUT, receiver_bandwdth$OFFSET, fieldValue);
    }

    private static final OfDouble tx_pulse_widthLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_pulse_width"));

    private static final long tx_pulse_width$OFFSET = 64;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_pulse_width
     * }
     */
    public double tx_pulse_width() {
        return struct.get(tx_pulse_widthLAYOUT, tx_pulse_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_pulse_width
     * }
     */
    public void tx_pulse_width(double fieldValue) {
        struct.set(tx_pulse_widthLAYOUT, tx_pulse_width$OFFSET, fieldValue);
    }

    private static final OfInt tx_pulse_type_idLAYOUT = (OfInt)LAYOUT.select(groupElement("tx_pulse_type_id"));

    private static final long tx_pulse_type_id$OFFSET = 72;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int tx_pulse_type_id
     * }
     */
    public int tx_pulse_type_id() {
        return struct.get(tx_pulse_type_idLAYOUT, tx_pulse_type_id$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int tx_pulse_type_id
     * }
     */
    public void tx_pulse_type_id(int fieldValue) {
        struct.set(tx_pulse_type_idLAYOUT, tx_pulse_type_id$OFFSET, fieldValue);
    }

    private static final OfInt tx_pulse_envlp_idLAYOUT = (OfInt)LAYOUT.select(groupElement("tx_pulse_envlp_id"));

    private static final long tx_pulse_envlp_id$OFFSET = 76;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int tx_pulse_envlp_id
     * }
     */
    public int tx_pulse_envlp_id() {
        return struct.get(tx_pulse_envlp_idLAYOUT, tx_pulse_envlp_id$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int tx_pulse_envlp_id
     * }
     */
    public void tx_pulse_envlp_id(int fieldValue) {
        struct.set(tx_pulse_envlp_idLAYOUT, tx_pulse_envlp_id$OFFSET, fieldValue);
    }

    private static final OfDouble tx_pulse_envlp_paramLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_pulse_envlp_param"));

    private static final long tx_pulse_envlp_param$OFFSET = 80;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_pulse_envlp_param
     * }
     */
    public double tx_pulse_envlp_param() {
        return struct.get(tx_pulse_envlp_paramLAYOUT, tx_pulse_envlp_param$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_pulse_envlp_param
     * }
     */
    public void tx_pulse_envlp_param(double fieldValue) {
        struct.set(tx_pulse_envlp_paramLAYOUT, tx_pulse_envlp_param$OFFSET, fieldValue);
    }

    private static final OfShort tx_pulse_modeLAYOUT = (OfShort)LAYOUT.select(groupElement("tx_pulse_mode"));

    private static final long tx_pulse_mode$OFFSET = 88;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short tx_pulse_mode
     * }
     */
    public short tx_pulse_mode() {
        return struct.get(tx_pulse_modeLAYOUT, tx_pulse_mode$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short tx_pulse_mode
     * }
     */
    public void tx_pulse_mode(short fieldValue) {
        struct.set(tx_pulse_modeLAYOUT, tx_pulse_mode$OFFSET, fieldValue);
    }

    private static final OfShort tx_pulse_reservedLAYOUT = (OfShort)LAYOUT.select(groupElement("tx_pulse_reserved"));

    private static final long tx_pulse_reserved$OFFSET = 90;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short tx_pulse_reserved
     * }
     */
    public short tx_pulse_reserved() {
        return struct.get(tx_pulse_reservedLAYOUT, tx_pulse_reserved$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short tx_pulse_reserved
     * }
     */
    public void tx_pulse_reserved(short fieldValue) {
        struct.set(tx_pulse_reservedLAYOUT, tx_pulse_reserved$OFFSET, fieldValue);
    }

    private static final OfDouble max_ping_rateLAYOUT = (OfDouble)LAYOUT.select(groupElement("max_ping_rate"));

    private static final long max_ping_rate$OFFSET = 96;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double max_ping_rate
     * }
     */
    public double max_ping_rate() {
        return struct.get(max_ping_rateLAYOUT, max_ping_rate$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double max_ping_rate
     * }
     */
    public void max_ping_rate(double fieldValue) {
        struct.set(max_ping_rateLAYOUT, max_ping_rate$OFFSET, fieldValue);
    }

    private static final OfDouble ping_periodLAYOUT = (OfDouble)LAYOUT.select(groupElement("ping_period"));

    private static final long ping_period$OFFSET = 104;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double ping_period
     * }
     */
    public double ping_period() {
        return struct.get(ping_periodLAYOUT, ping_period$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double ping_period
     * }
     */
    public void ping_period(double fieldValue) {
        struct.set(ping_periodLAYOUT, ping_period$OFFSET, fieldValue);
    }

    private static final OfDouble rangeLAYOUT = (OfDouble)LAYOUT.select(groupElement("range"));

    private static final long range$OFFSET = 112;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double range
     * }
     */
    public double range() {
        return struct.get(rangeLAYOUT, range$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double range
     * }
     */
    public void range(double fieldValue) {
        struct.set(rangeLAYOUT, range$OFFSET, fieldValue);
    }

    private static final OfDouble powerLAYOUT = (OfDouble)LAYOUT.select(groupElement("power"));

    private static final long power$OFFSET = 120;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double power
     * }
     */
    public double power() {
        return struct.get(powerLAYOUT, power$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double power
     * }
     */
    public void power(double fieldValue) {
        struct.set(powerLAYOUT, power$OFFSET, fieldValue);
    }

    private static final OfDouble gainLAYOUT = (OfDouble)LAYOUT.select(groupElement("gain"));

    private static final long gain$OFFSET = 128;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double gain
     * }
     */
    public double gain() {
        return struct.get(gainLAYOUT, gain$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double gain
     * }
     */
    public void gain(double fieldValue) {
        struct.set(gainLAYOUT, gain$OFFSET, fieldValue);
    }

    private static final OfInt control_flagsLAYOUT = (OfInt)LAYOUT.select(groupElement("control_flags"));

    private static final long control_flags$OFFSET = 136;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int control_flags
     * }
     */
    public int control_flags() {
        return struct.get(control_flagsLAYOUT, control_flags$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int control_flags
     * }
     */
    public void control_flags(int fieldValue) {
        struct.set(control_flagsLAYOUT, control_flags$OFFSET, fieldValue);
    }

    private static final OfInt projector_idLAYOUT = (OfInt)LAYOUT.select(groupElement("projector_id"));

    private static final long projector_id$OFFSET = 140;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int projector_id
     * }
     */
    public int projector_id() {
        return struct.get(projector_idLAYOUT, projector_id$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int projector_id
     * }
     */
    public void projector_id(int fieldValue) {
        struct.set(projector_idLAYOUT, projector_id$OFFSET, fieldValue);
    }

    private static final OfDouble projector_steer_angl_vertLAYOUT = (OfDouble)LAYOUT.select(groupElement("projector_steer_angl_vert"));

    private static final long projector_steer_angl_vert$OFFSET = 144;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double projector_steer_angl_vert
     * }
     */
    public double projector_steer_angl_vert() {
        return struct.get(projector_steer_angl_vertLAYOUT, projector_steer_angl_vert$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double projector_steer_angl_vert
     * }
     */
    public void projector_steer_angl_vert(double fieldValue) {
        struct.set(projector_steer_angl_vertLAYOUT, projector_steer_angl_vert$OFFSET, fieldValue);
    }

    private static final OfDouble projector_steer_angl_horzLAYOUT = (OfDouble)LAYOUT.select(groupElement("projector_steer_angl_horz"));

    private static final long projector_steer_angl_horz$OFFSET = 152;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double projector_steer_angl_horz
     * }
     */
    public double projector_steer_angl_horz() {
        return struct.get(projector_steer_angl_horzLAYOUT, projector_steer_angl_horz$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double projector_steer_angl_horz
     * }
     */
    public void projector_steer_angl_horz(double fieldValue) {
        struct.set(projector_steer_angl_horzLAYOUT, projector_steer_angl_horz$OFFSET, fieldValue);
    }

    private static final OfDouble projector_beam_wdth_vertLAYOUT = (OfDouble)LAYOUT.select(groupElement("projector_beam_wdth_vert"));

    private static final long projector_beam_wdth_vert$OFFSET = 160;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double projector_beam_wdth_vert
     * }
     */
    public double projector_beam_wdth_vert() {
        return struct.get(projector_beam_wdth_vertLAYOUT, projector_beam_wdth_vert$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double projector_beam_wdth_vert
     * }
     */
    public void projector_beam_wdth_vert(double fieldValue) {
        struct.set(projector_beam_wdth_vertLAYOUT, projector_beam_wdth_vert$OFFSET, fieldValue);
    }

    private static final OfDouble projector_beam_wdth_horzLAYOUT = (OfDouble)LAYOUT.select(groupElement("projector_beam_wdth_horz"));

    private static final long projector_beam_wdth_horz$OFFSET = 168;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double projector_beam_wdth_horz
     * }
     */
    public double projector_beam_wdth_horz() {
        return struct.get(projector_beam_wdth_horzLAYOUT, projector_beam_wdth_horz$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double projector_beam_wdth_horz
     * }
     */
    public void projector_beam_wdth_horz(double fieldValue) {
        struct.set(projector_beam_wdth_horzLAYOUT, projector_beam_wdth_horz$OFFSET, fieldValue);
    }

    private static final OfDouble projector_beam_focal_ptLAYOUT = (OfDouble)LAYOUT.select(groupElement("projector_beam_focal_pt"));

    private static final long projector_beam_focal_pt$OFFSET = 176;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double projector_beam_focal_pt
     * }
     */
    public double projector_beam_focal_pt() {
        return struct.get(projector_beam_focal_ptLAYOUT, projector_beam_focal_pt$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double projector_beam_focal_pt
     * }
     */
    public void projector_beam_focal_pt(double fieldValue) {
        struct.set(projector_beam_focal_ptLAYOUT, projector_beam_focal_pt$OFFSET, fieldValue);
    }

    private static final OfInt projector_beam_weighting_window_typeLAYOUT = (OfInt)LAYOUT.select(groupElement("projector_beam_weighting_window_type"));

    private static final long projector_beam_weighting_window_type$OFFSET = 184;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int projector_beam_weighting_window_type
     * }
     */
    public int projector_beam_weighting_window_type() {
        return struct.get(projector_beam_weighting_window_typeLAYOUT, projector_beam_weighting_window_type$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int projector_beam_weighting_window_type
     * }
     */
    public void projector_beam_weighting_window_type(int fieldValue) {
        struct.set(projector_beam_weighting_window_typeLAYOUT, projector_beam_weighting_window_type$OFFSET, fieldValue);
    }

    private static final OfDouble projector_beam_weighting_window_paramLAYOUT = (OfDouble)LAYOUT.select(groupElement("projector_beam_weighting_window_param"));

    private static final long projector_beam_weighting_window_param$OFFSET = 192;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double projector_beam_weighting_window_param
     * }
     */
    public double projector_beam_weighting_window_param() {
        return struct.get(projector_beam_weighting_window_paramLAYOUT, projector_beam_weighting_window_param$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double projector_beam_weighting_window_param
     * }
     */
    public void projector_beam_weighting_window_param(double fieldValue) {
        struct.set(projector_beam_weighting_window_paramLAYOUT, projector_beam_weighting_window_param$OFFSET, fieldValue);
    }

    private static final OfInt transmit_flagsLAYOUT = (OfInt)LAYOUT.select(groupElement("transmit_flags"));

    private static final long transmit_flags$OFFSET = 200;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int transmit_flags
     * }
     */
    public int transmit_flags() {
        return struct.get(transmit_flagsLAYOUT, transmit_flags$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int transmit_flags
     * }
     */
    public void transmit_flags(int fieldValue) {
        struct.set(transmit_flagsLAYOUT, transmit_flags$OFFSET, fieldValue);
    }

    private static final OfInt hydrophone_idLAYOUT = (OfInt)LAYOUT.select(groupElement("hydrophone_id"));

    private static final long hydrophone_id$OFFSET = 204;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int hydrophone_id
     * }
     */
    public int hydrophone_id() {
        return struct.get(hydrophone_idLAYOUT, hydrophone_id$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int hydrophone_id
     * }
     */
    public void hydrophone_id(int fieldValue) {
        struct.set(hydrophone_idLAYOUT, hydrophone_id$OFFSET, fieldValue);
    }

    private static final OfInt receiving_beam_weighting_window_typeLAYOUT = (OfInt)LAYOUT.select(groupElement("receiving_beam_weighting_window_type"));

    private static final long receiving_beam_weighting_window_type$OFFSET = 208;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int receiving_beam_weighting_window_type
     * }
     */
    public int receiving_beam_weighting_window_type() {
        return struct.get(receiving_beam_weighting_window_typeLAYOUT, receiving_beam_weighting_window_type$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int receiving_beam_weighting_window_type
     * }
     */
    public void receiving_beam_weighting_window_type(int fieldValue) {
        struct.set(receiving_beam_weighting_window_typeLAYOUT, receiving_beam_weighting_window_type$OFFSET, fieldValue);
    }

    private static final OfDouble receiving_beam_weighting_window_paramLAYOUT = (OfDouble)LAYOUT.select(groupElement("receiving_beam_weighting_window_param"));

    private static final long receiving_beam_weighting_window_param$OFFSET = 216;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double receiving_beam_weighting_window_param
     * }
     */
    public double receiving_beam_weighting_window_param() {
        return struct.get(receiving_beam_weighting_window_paramLAYOUT, receiving_beam_weighting_window_param$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double receiving_beam_weighting_window_param
     * }
     */
    public void receiving_beam_weighting_window_param(double fieldValue) {
        struct.set(receiving_beam_weighting_window_paramLAYOUT, receiving_beam_weighting_window_param$OFFSET, fieldValue);
    }

    private static final OfInt receive_flagsLAYOUT = (OfInt)LAYOUT.select(groupElement("receive_flags"));

    private static final long receive_flags$OFFSET = 224;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int receive_flags
     * }
     */
    public int receive_flags() {
        return struct.get(receive_flagsLAYOUT, receive_flags$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int receive_flags
     * }
     */
    public void receive_flags(int fieldValue) {
        struct.set(receive_flagsLAYOUT, receive_flags$OFFSET, fieldValue);
    }

    private static final OfDouble receive_beam_widthLAYOUT = (OfDouble)LAYOUT.select(groupElement("receive_beam_width"));

    private static final long receive_beam_width$OFFSET = 232;

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

    private static final OfDouble range_filt_minLAYOUT = (OfDouble)LAYOUT.select(groupElement("range_filt_min"));

    private static final long range_filt_min$OFFSET = 240;

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

    private static final long range_filt_max$OFFSET = 248;

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

    private static final long depth_filt_min$OFFSET = 256;

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

    private static final long depth_filt_max$OFFSET = 264;

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

    private static final OfDouble absorptionLAYOUT = (OfDouble)LAYOUT.select(groupElement("absorption"));

    private static final long absorption$OFFSET = 272;

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

    private static final OfDouble sound_velocityLAYOUT = (OfDouble)LAYOUT.select(groupElement("sound_velocity"));

    private static final long sound_velocity$OFFSET = 280;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double sound_velocity
     * }
     */
    public double sound_velocity() {
        return struct.get(sound_velocityLAYOUT, sound_velocity$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double sound_velocity
     * }
     */
    public void sound_velocity(double fieldValue) {
        struct.set(sound_velocityLAYOUT, sound_velocity$OFFSET, fieldValue);
    }

    private static final OfByte sv_sourceLAYOUT = (OfByte)LAYOUT.select(groupElement("sv_source"));

    private static final long sv_source$OFFSET = 288;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char sv_source
     * }
     */
    public byte sv_source() {
        return struct.get(sv_sourceLAYOUT, sv_source$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char sv_source
     * }
     */
    public void sv_source(byte fieldValue) {
        struct.set(sv_sourceLAYOUT, sv_source$OFFSET, fieldValue);
    }

    private static final OfDouble spreadingLAYOUT = (OfDouble)LAYOUT.select(groupElement("spreading"));

    private static final long spreading$OFFSET = 296;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double spreading
     * }
     */
    public double spreading() {
        return struct.get(spreadingLAYOUT, spreading$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double spreading
     * }
     */
    public void spreading(double fieldValue) {
        struct.set(spreadingLAYOUT, spreading$OFFSET, fieldValue);
    }

    private static final OfShort beam_spacing_modeLAYOUT = (OfShort)LAYOUT.select(groupElement("beam_spacing_mode"));

    private static final long beam_spacing_mode$OFFSET = 304;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short beam_spacing_mode
     * }
     */
    public short beam_spacing_mode() {
        return struct.get(beam_spacing_modeLAYOUT, beam_spacing_mode$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short beam_spacing_mode
     * }
     */
    public void beam_spacing_mode(short fieldValue) {
        struct.set(beam_spacing_modeLAYOUT, beam_spacing_mode$OFFSET, fieldValue);
    }

    private static final OfShort sonar_source_modeLAYOUT = (OfShort)LAYOUT.select(groupElement("sonar_source_mode"));

    private static final long sonar_source_mode$OFFSET = 306;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short sonar_source_mode
     * }
     */
    public short sonar_source_mode() {
        return struct.get(sonar_source_modeLAYOUT, sonar_source_mode$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short sonar_source_mode
     * }
     */
    public void sonar_source_mode(short fieldValue) {
        struct.set(sonar_source_modeLAYOUT, sonar_source_mode$OFFSET, fieldValue);
    }

    private static final OfByte coverage_modeLAYOUT = (OfByte)LAYOUT.select(groupElement("coverage_mode"));

    private static final long coverage_mode$OFFSET = 308;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char coverage_mode
     * }
     */
    public byte coverage_mode() {
        return struct.get(coverage_modeLAYOUT, coverage_mode$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char coverage_mode
     * }
     */
    public void coverage_mode(byte fieldValue) {
        struct.set(coverage_modeLAYOUT, coverage_mode$OFFSET, fieldValue);
    }

    private static final OfDouble coverage_angleLAYOUT = (OfDouble)LAYOUT.select(groupElement("coverage_angle"));

    private static final long coverage_angle$OFFSET = 312;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double coverage_angle
     * }
     */
    public double coverage_angle() {
        return struct.get(coverage_angleLAYOUT, coverage_angle$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double coverage_angle
     * }
     */
    public void coverage_angle(double fieldValue) {
        struct.set(coverage_angleLAYOUT, coverage_angle$OFFSET, fieldValue);
    }

    private static final OfDouble horizontal_receiver_steering_angleLAYOUT = (OfDouble)LAYOUT.select(groupElement("horizontal_receiver_steering_angle"));

    private static final long horizontal_receiver_steering_angle$OFFSET = 320;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double horizontal_receiver_steering_angle
     * }
     */
    public double horizontal_receiver_steering_angle() {
        return struct.get(horizontal_receiver_steering_angleLAYOUT, horizontal_receiver_steering_angle$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double horizontal_receiver_steering_angle
     * }
     */
    public void horizontal_receiver_steering_angle(double fieldValue) {
        struct.set(horizontal_receiver_steering_angleLAYOUT, horizontal_receiver_steering_angle$OFFSET, fieldValue);
    }

    private static final SequenceLayout reserved_2LAYOUT = (SequenceLayout)LAYOUT.select(groupElement("reserved_2"));

    private static final long reserved_2$OFFSET = 328;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char reserved_2[3]
     * }
     */
    public MemorySegment reserved_2() {
        return struct.asSlice(reserved_2$OFFSET, reserved_2LAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char reserved_2[3]
     * }
     */
    public void reserved_2(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, reserved_2$OFFSET, reserved_2LAYOUT.byteSize());
    }

    private static final VarHandle reserved_2$ELEM_HANDLE = reserved_2LAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char reserved_2[3]
     * }
     */
    public byte reserved_2(long index0) {
        return (byte)reserved_2$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char reserved_2[3]
     * }
     */
    public void reserved_2(long index0, byte fieldValue) {
        reserved_2$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final OfInt uncertainty_typeLAYOUT = (OfInt)LAYOUT.select(groupElement("uncertainty_type"));

    private static final long uncertainty_type$OFFSET = 332;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int uncertainty_type
     * }
     */
    public int uncertainty_type() {
        return struct.get(uncertainty_typeLAYOUT, uncertainty_type$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int uncertainty_type
     * }
     */
    public void uncertainty_type(int fieldValue) {
        struct.set(uncertainty_typeLAYOUT, uncertainty_type$OFFSET, fieldValue);
    }

    private static final OfDouble transmitter_steering_angleLAYOUT = (OfDouble)LAYOUT.select(groupElement("transmitter_steering_angle"));

    private static final long transmitter_steering_angle$OFFSET = 336;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double transmitter_steering_angle
     * }
     */
    public double transmitter_steering_angle() {
        return struct.get(transmitter_steering_angleLAYOUT, transmitter_steering_angle$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double transmitter_steering_angle
     * }
     */
    public void transmitter_steering_angle(double fieldValue) {
        struct.set(transmitter_steering_angleLAYOUT, transmitter_steering_angle$OFFSET, fieldValue);
    }

    private static final OfDouble applied_rollLAYOUT = (OfDouble)LAYOUT.select(groupElement("applied_roll"));

    private static final long applied_roll$OFFSET = 344;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double applied_roll
     * }
     */
    public double applied_roll() {
        return struct.get(applied_rollLAYOUT, applied_roll$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double applied_roll
     * }
     */
    public void applied_roll(double fieldValue) {
        struct.set(applied_rollLAYOUT, applied_roll$OFFSET, fieldValue);
    }

    private static final OfShort detection_algorithmLAYOUT = (OfShort)LAYOUT.select(groupElement("detection_algorithm"));

    private static final long detection_algorithm$OFFSET = 352;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short detection_algorithm
     * }
     */
    public short detection_algorithm() {
        return struct.get(detection_algorithmLAYOUT, detection_algorithm$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short detection_algorithm
     * }
     */
    public void detection_algorithm(short fieldValue) {
        struct.set(detection_algorithmLAYOUT, detection_algorithm$OFFSET, fieldValue);
    }

    private static final OfInt detection_flagsLAYOUT = (OfInt)LAYOUT.select(groupElement("detection_flags"));

    private static final long detection_flags$OFFSET = 356;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int detection_flags
     * }
     */
    public int detection_flags() {
        return struct.get(detection_flagsLAYOUT, detection_flags$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int detection_flags
     * }
     */
    public void detection_flags(int fieldValue) {
        struct.set(detection_flagsLAYOUT, detection_flags$OFFSET, fieldValue);
    }

    private static final SequenceLayout device_descriptionLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("device_description"));

    private static final long device_description$OFFSET = 360;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char device_description[60]
     * }
     */
    public MemorySegment device_description() {
        return struct.asSlice(device_description$OFFSET, device_descriptionLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char device_description[60]
     * }
     */
    public void device_description(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, device_description$OFFSET, device_descriptionLAYOUT.byteSize());
    }

    private static final VarHandle device_description$ELEM_HANDLE = device_descriptionLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * char device_description[60]
     * }
     */
    public byte device_description(long index0) {
        return (byte)device_description$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * char device_description[60]
     * }
     */
    public void device_description(long index0, byte fieldValue) {
        device_description$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final SequenceLayout reserved_7027LAYOUT = (SequenceLayout)LAYOUT.select(groupElement("reserved_7027"));

    private static final long reserved_7027$OFFSET = 420;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char reserved_7027[420]
     * }
     */
    public MemorySegment reserved_7027() {
        return struct.asSlice(reserved_7027$OFFSET, reserved_7027LAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char reserved_7027[420]
     * }
     */
    public void reserved_7027(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, reserved_7027$OFFSET, reserved_7027LAYOUT.byteSize());
    }

    private static final VarHandle reserved_7027$ELEM_HANDLE = reserved_7027LAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char reserved_7027[420]
     * }
     */
    public byte reserved_7027(long index0) {
        return (byte)reserved_7027$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char reserved_7027[420]
     * }
     */
    public void reserved_7027(long index0, byte fieldValue) {
        reserved_7027$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final SequenceLayout reserved_3LAYOUT = (SequenceLayout)LAYOUT.select(groupElement("reserved_3"));

    private static final long reserved_3$OFFSET = 840;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char reserved_3[32]
     * }
     */
    public MemorySegment reserved_3() {
        return struct.asSlice(reserved_3$OFFSET, reserved_3LAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char reserved_3[32]
     * }
     */
    public void reserved_3(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, reserved_3$OFFSET, reserved_3LAYOUT.byteSize());
    }

    private static final VarHandle reserved_3$ELEM_HANDLE = reserved_3LAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char reserved_3[32]
     * }
     */
    public byte reserved_3(long index0) {
        return (byte)reserved_3$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char reserved_3[32]
     * }
     */
    public void reserved_3(long index0, byte fieldValue) {
        reserved_3$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }
}
