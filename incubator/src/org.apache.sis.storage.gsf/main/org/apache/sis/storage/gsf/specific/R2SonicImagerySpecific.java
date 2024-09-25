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

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final SequenceLayout model_numberLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("model_number"));

    private static final long model_number$OFFSET = 0;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char model_number[12]
     * }
     */
    public MemorySegment model_number() {
        return struct.asSlice(model_number$OFFSET, model_numberLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char model_number[12]
     * }
     */
    public void model_number(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, model_number$OFFSET, model_numberLAYOUT.byteSize());
    }

    private static final VarHandle model_number$ELEM_HANDLE = model_numberLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char model_number[12]
     * }
     */
    public byte model_number(long index0) {
        return (byte)model_number$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char model_number[12]
     * }
     */
    public void model_number(long index0, byte fieldValue) {
        model_number$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final SequenceLayout serial_numberLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("serial_number"));

    private static final long serial_number$OFFSET = 12;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char serial_number[12]
     * }
     */
    public MemorySegment serial_number() {
        return struct.asSlice(serial_number$OFFSET, serial_numberLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char serial_number[12]
     * }
     */
    public void serial_number(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, serial_number$OFFSET, serial_numberLAYOUT.byteSize());
    }

    private static final VarHandle serial_number$ELEM_HANDLE = serial_numberLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char serial_number[12]
     * }
     */
    public byte serial_number(long index0) {
        return (byte)serial_number$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char serial_number[12]
     * }
     */
    public void serial_number(long index0, byte fieldValue) {
        serial_number$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final GroupLayout dg_timeLAYOUT = (GroupLayout)LAYOUT.select(groupElement("dg_time"));

    private static final long dg_time$OFFSET = 24;

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

    private static final long ping_number$OFFSET = 40;

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

    private static final OfDouble ping_periodLAYOUT = (OfDouble)LAYOUT.select(groupElement("ping_period"));

    private static final long ping_period$OFFSET = 48;

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

    private static final OfDouble sound_speedLAYOUT = (OfDouble)LAYOUT.select(groupElement("sound_speed"));

    private static final long sound_speed$OFFSET = 56;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double sound_speed
     * }
     */
    public double sound_speed() {
        return struct.get(sound_speedLAYOUT, sound_speed$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double sound_speed
     * }
     */
    public void sound_speed(double fieldValue) {
        struct.set(sound_speedLAYOUT, sound_speed$OFFSET, fieldValue);
    }

    private static final OfDouble frequencyLAYOUT = (OfDouble)LAYOUT.select(groupElement("frequency"));

    private static final long frequency$OFFSET = 64;

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

    private static final OfDouble tx_powerLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_power"));

    private static final long tx_power$OFFSET = 72;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_power
     * }
     */
    public double tx_power() {
        return struct.get(tx_powerLAYOUT, tx_power$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_power
     * }
     */
    public void tx_power(double fieldValue) {
        struct.set(tx_powerLAYOUT, tx_power$OFFSET, fieldValue);
    }

    private static final OfDouble tx_pulse_widthLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_pulse_width"));

    private static final long tx_pulse_width$OFFSET = 80;

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

    private static final OfDouble tx_beamwidth_vertLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_beamwidth_vert"));

    private static final long tx_beamwidth_vert$OFFSET = 88;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_beamwidth_vert
     * }
     */
    public double tx_beamwidth_vert() {
        return struct.get(tx_beamwidth_vertLAYOUT, tx_beamwidth_vert$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_beamwidth_vert
     * }
     */
    public void tx_beamwidth_vert(double fieldValue) {
        struct.set(tx_beamwidth_vertLAYOUT, tx_beamwidth_vert$OFFSET, fieldValue);
    }

    private static final OfDouble tx_beamwidth_horizLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_beamwidth_horiz"));

    private static final long tx_beamwidth_horiz$OFFSET = 96;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_beamwidth_horiz
     * }
     */
    public double tx_beamwidth_horiz() {
        return struct.get(tx_beamwidth_horizLAYOUT, tx_beamwidth_horiz$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_beamwidth_horiz
     * }
     */
    public void tx_beamwidth_horiz(double fieldValue) {
        struct.set(tx_beamwidth_horizLAYOUT, tx_beamwidth_horiz$OFFSET, fieldValue);
    }

    private static final OfDouble tx_steering_vertLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_steering_vert"));

    private static final long tx_steering_vert$OFFSET = 104;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_steering_vert
     * }
     */
    public double tx_steering_vert() {
        return struct.get(tx_steering_vertLAYOUT, tx_steering_vert$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_steering_vert
     * }
     */
    public void tx_steering_vert(double fieldValue) {
        struct.set(tx_steering_vertLAYOUT, tx_steering_vert$OFFSET, fieldValue);
    }

    private static final OfDouble tx_steering_horizLAYOUT = (OfDouble)LAYOUT.select(groupElement("tx_steering_horiz"));

    private static final long tx_steering_horiz$OFFSET = 112;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tx_steering_horiz
     * }
     */
    public double tx_steering_horiz() {
        return struct.get(tx_steering_horizLAYOUT, tx_steering_horiz$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tx_steering_horiz
     * }
     */
    public void tx_steering_horiz(double fieldValue) {
        struct.set(tx_steering_horizLAYOUT, tx_steering_horiz$OFFSET, fieldValue);
    }

    private static final OfInt tx_misc_infoLAYOUT = (OfInt)LAYOUT.select(groupElement("tx_misc_info"));

    private static final long tx_misc_info$OFFSET = 120;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int tx_misc_info
     * }
     */
    public int tx_misc_info() {
        return struct.get(tx_misc_infoLAYOUT, tx_misc_info$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int tx_misc_info
     * }
     */
    public void tx_misc_info(int fieldValue) {
        struct.set(tx_misc_infoLAYOUT, tx_misc_info$OFFSET, fieldValue);
    }

    private static final OfDouble rx_bandwidthLAYOUT = (OfDouble)LAYOUT.select(groupElement("rx_bandwidth"));

    private static final long rx_bandwidth$OFFSET = 128;

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

    private static final OfDouble rx_sample_rateLAYOUT = (OfDouble)LAYOUT.select(groupElement("rx_sample_rate"));

    private static final long rx_sample_rate$OFFSET = 136;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double rx_sample_rate
     * }
     */
    public double rx_sample_rate() {
        return struct.get(rx_sample_rateLAYOUT, rx_sample_rate$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double rx_sample_rate
     * }
     */
    public void rx_sample_rate(double fieldValue) {
        struct.set(rx_sample_rateLAYOUT, rx_sample_rate$OFFSET, fieldValue);
    }

    private static final OfDouble rx_rangeLAYOUT = (OfDouble)LAYOUT.select(groupElement("rx_range"));

    private static final long rx_range$OFFSET = 144;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double rx_range
     * }
     */
    public double rx_range() {
        return struct.get(rx_rangeLAYOUT, rx_range$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double rx_range
     * }
     */
    public void rx_range(double fieldValue) {
        struct.set(rx_rangeLAYOUT, rx_range$OFFSET, fieldValue);
    }

    private static final OfDouble rx_gainLAYOUT = (OfDouble)LAYOUT.select(groupElement("rx_gain"));

    private static final long rx_gain$OFFSET = 152;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double rx_gain
     * }
     */
    public double rx_gain() {
        return struct.get(rx_gainLAYOUT, rx_gain$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double rx_gain
     * }
     */
    public void rx_gain(double fieldValue) {
        struct.set(rx_gainLAYOUT, rx_gain$OFFSET, fieldValue);
    }

    private static final OfDouble rx_spreadingLAYOUT = (OfDouble)LAYOUT.select(groupElement("rx_spreading"));

    private static final long rx_spreading$OFFSET = 160;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double rx_spreading
     * }
     */
    public double rx_spreading() {
        return struct.get(rx_spreadingLAYOUT, rx_spreading$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double rx_spreading
     * }
     */
    public void rx_spreading(double fieldValue) {
        struct.set(rx_spreadingLAYOUT, rx_spreading$OFFSET, fieldValue);
    }

    private static final OfDouble rx_absorptionLAYOUT = (OfDouble)LAYOUT.select(groupElement("rx_absorption"));

    private static final long rx_absorption$OFFSET = 168;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double rx_absorption
     * }
     */
    public double rx_absorption() {
        return struct.get(rx_absorptionLAYOUT, rx_absorption$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double rx_absorption
     * }
     */
    public void rx_absorption(double fieldValue) {
        struct.set(rx_absorptionLAYOUT, rx_absorption$OFFSET, fieldValue);
    }

    private static final OfDouble rx_mount_tiltLAYOUT = (OfDouble)LAYOUT.select(groupElement("rx_mount_tilt"));

    private static final long rx_mount_tilt$OFFSET = 176;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double rx_mount_tilt
     * }
     */
    public double rx_mount_tilt() {
        return struct.get(rx_mount_tiltLAYOUT, rx_mount_tilt$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double rx_mount_tilt
     * }
     */
    public void rx_mount_tilt(double fieldValue) {
        struct.set(rx_mount_tiltLAYOUT, rx_mount_tilt$OFFSET, fieldValue);
    }

    private static final OfInt rx_misc_infoLAYOUT = (OfInt)LAYOUT.select(groupElement("rx_misc_info"));

    private static final long rx_misc_info$OFFSET = 184;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int rx_misc_info
     * }
     */
    public int rx_misc_info() {
        return struct.get(rx_misc_infoLAYOUT, rx_misc_info$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int rx_misc_info
     * }
     */
    public void rx_misc_info(int fieldValue) {
        struct.set(rx_misc_infoLAYOUT, rx_misc_info$OFFSET, fieldValue);
    }

    private static final OfShort reservedLAYOUT = (OfShort)LAYOUT.select(groupElement("reserved"));

    private static final long reserved$OFFSET = 188;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short reserved
     * }
     */
    public short reserved() {
        return struct.get(reservedLAYOUT, reserved$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short reserved
     * }
     */
    public void reserved(short fieldValue) {
        struct.set(reservedLAYOUT, reserved$OFFSET, fieldValue);
    }

    private static final OfShort num_beamsLAYOUT = (OfShort)LAYOUT.select(groupElement("num_beams"));

    private static final long num_beams$OFFSET = 190;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short num_beams
     * }
     */
    public short num_beams() {
        return struct.get(num_beamsLAYOUT, num_beams$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short num_beams
     * }
     */
    public void num_beams(short fieldValue) {
        struct.set(num_beamsLAYOUT, num_beams$OFFSET, fieldValue);
    }

    private static final SequenceLayout more_infoLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("more_info"));

    private static final long more_info$OFFSET = 192;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double more_info[6]
     * }
     */
    public MemorySegment more_info() {
        return struct.asSlice(more_info$OFFSET, more_infoLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double more_info[6]
     * }
     */
    public void more_info(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, more_info$OFFSET, more_infoLAYOUT.byteSize());
    }

    private static final VarHandle more_info$ELEM_HANDLE = more_infoLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * double more_info[6]
     * }
     */
    public double more_info(long index0) {
        return (double)more_info$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * double more_info[6]
     * }
     */
    public void more_info(long index0, double fieldValue) {
        more_info$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    private static final long spare$OFFSET = 240;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare[32]
     * }
     */
    public MemorySegment spare() {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char spare[32]
     * }
     */
    public void spare(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char spare[32]
     * }
     */
    public byte spare(long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char spare[32]
     * }
     */
    public void spare(long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }
}
