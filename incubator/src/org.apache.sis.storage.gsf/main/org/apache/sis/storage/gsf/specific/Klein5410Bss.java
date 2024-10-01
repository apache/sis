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
public final class Klein5410Bss extends StructClass {
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("data_source"),
        GSF.C_INT.withName("side"),
        GSF.C_INT.withName("model_number"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("acoustic_frequency"),
        GSF.C_DOUBLE.withName("sampling_frequency"),
        GSF.C_INT.withName("ping_number"),
        GSF.C_INT.withName("num_samples"),
        GSF.C_INT.withName("num_raa_samples"),
        GSF.C_INT.withName("error_flags"),
        GSF.C_INT.withName("range"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("fish_depth"),
        GSF.C_DOUBLE.withName("fish_altitude"),
        GSF.C_DOUBLE.withName("sound_speed"),
        GSF.C_INT.withName("tx_waveform"),
        GSF.C_INT.withName("altimeter"),
        GSF.C_INT.withName("raw_data_config"),
        MemoryLayout.sequenceLayout(32, GSF.C_CHAR).withName("spare"),
        MemoryLayout.paddingLayout(4)
    ).withName("t_gsfKlein5410BssSpecific");

    public Klein5410Bss(MemorySegment struct) {
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

    private static final OfDouble acoustic_frequencyLAYOUT = (OfDouble)LAYOUT.select(groupElement("acoustic_frequency"));

    private static final long acoustic_frequency$OFFSET = 16;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double acoustic_frequency
     * }
     */
    public double acoustic_frequency() {
        return struct.get(acoustic_frequencyLAYOUT, acoustic_frequency$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double acoustic_frequency
     * }
     */
    public void acoustic_frequency(double fieldValue) {
        struct.set(acoustic_frequencyLAYOUT, acoustic_frequency$OFFSET, fieldValue);
    }

    private static final OfDouble sampling_frequencyLAYOUT = (OfDouble)LAYOUT.select(groupElement("sampling_frequency"));

    private static final long sampling_frequency$OFFSET = 24;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double sampling_frequency
     * }
     */
    public double sampling_frequency() {
        return struct.get(sampling_frequencyLAYOUT, sampling_frequency$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double sampling_frequency
     * }
     */
    public void sampling_frequency(double fieldValue) {
        struct.set(sampling_frequencyLAYOUT, sampling_frequency$OFFSET, fieldValue);
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

    private static final OfInt num_samplesLAYOUT = (OfInt)LAYOUT.select(groupElement("num_samples"));

    private static final long num_samples$OFFSET = 36;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int num_samples
     * }
     */
    public int num_samples() {
        return struct.get(num_samplesLAYOUT, num_samples$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int num_samples
     * }
     */
    public void num_samples(int fieldValue) {
        struct.set(num_samplesLAYOUT, num_samples$OFFSET, fieldValue);
    }

    private static final OfInt num_raa_samplesLAYOUT = (OfInt)LAYOUT.select(groupElement("num_raa_samples"));

    private static final long num_raa_samples$OFFSET = 40;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int num_raa_samples
     * }
     */
    public int num_raa_samples() {
        return struct.get(num_raa_samplesLAYOUT, num_raa_samples$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int num_raa_samples
     * }
     */
    public void num_raa_samples(int fieldValue) {
        struct.set(num_raa_samplesLAYOUT, num_raa_samples$OFFSET, fieldValue);
    }

    private static final OfInt error_flagsLAYOUT = (OfInt)LAYOUT.select(groupElement("error_flags"));

    private static final long error_flags$OFFSET = 44;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int error_flags
     * }
     */
    public int error_flags() {
        return struct.get(error_flagsLAYOUT, error_flags$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int error_flags
     * }
     */
    public void error_flags(int fieldValue) {
        struct.set(error_flagsLAYOUT, error_flags$OFFSET, fieldValue);
    }

    private static final OfInt rangeLAYOUT = (OfInt)LAYOUT.select(groupElement("range"));

    private static final long range$OFFSET = 48;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int range
     * }
     */
    public int range() {
        return struct.get(rangeLAYOUT, range$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int range
     * }
     */
    public void range(int fieldValue) {
        struct.set(rangeLAYOUT, range$OFFSET, fieldValue);
    }

    private static final OfDouble fish_depthLAYOUT = (OfDouble)LAYOUT.select(groupElement("fish_depth"));

    private static final long fish_depth$OFFSET = 56;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double fish_depth
     * }
     */
    public double fish_depth() {
        return struct.get(fish_depthLAYOUT, fish_depth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double fish_depth
     * }
     */
    public void fish_depth(double fieldValue) {
        struct.set(fish_depthLAYOUT, fish_depth$OFFSET, fieldValue);
    }

    private static final OfDouble fish_altitudeLAYOUT = (OfDouble)LAYOUT.select(groupElement("fish_altitude"));

    private static final long fish_altitude$OFFSET = 64;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double fish_altitude
     * }
     */
    public double fish_altitude() {
        return struct.get(fish_altitudeLAYOUT, fish_altitude$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double fish_altitude
     * }
     */
    public void fish_altitude(double fieldValue) {
        struct.set(fish_altitudeLAYOUT, fish_altitude$OFFSET, fieldValue);
    }

    private static final OfDouble sound_speedLAYOUT = (OfDouble)LAYOUT.select(groupElement("sound_speed"));

    private static final long sound_speed$OFFSET = 72;

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

    private static final OfInt tx_waveformLAYOUT = (OfInt)LAYOUT.select(groupElement("tx_waveform"));

    private static final long tx_waveform$OFFSET = 80;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int tx_waveform
     * }
     */
    public int tx_waveform() {
        return struct.get(tx_waveformLAYOUT, tx_waveform$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int tx_waveform
     * }
     */
    public void tx_waveform(int fieldValue) {
        struct.set(tx_waveformLAYOUT, tx_waveform$OFFSET, fieldValue);
    }

    private static final OfInt altimeterLAYOUT = (OfInt)LAYOUT.select(groupElement("altimeter"));

    private static final long altimeter$OFFSET = 84;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int altimeter
     * }
     */
    public int altimeter() {
        return struct.get(altimeterLAYOUT, altimeter$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int altimeter
     * }
     */
    public void altimeter(int fieldValue) {
        struct.set(altimeterLAYOUT, altimeter$OFFSET, fieldValue);
    }

    private static final OfInt raw_data_configLAYOUT = (OfInt)LAYOUT.select(groupElement("raw_data_config"));

    private static final long raw_data_config$OFFSET = 88;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int raw_data_config
     * }
     */
    public int raw_data_config() {
        return struct.get(raw_data_configLAYOUT, raw_data_config$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int raw_data_config
     * }
     */
    public void raw_data_config(int fieldValue) {
        struct.set(raw_data_configLAYOUT, raw_data_config$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    private static final long spare$OFFSET = 92;

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
