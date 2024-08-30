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
public final class Klein5410BssSpecific extends StructClass {

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

    public Klein5410BssSpecific(MemorySegment struct) {
        super(struct);
    }

    public Klein5410BssSpecific(SegmentAllocator allocator) {
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

    private static final OfDouble acoustic_frequencyLAYOUT = (OfDouble)LAYOUT.select(groupElement("acoustic_frequency"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double acoustic_frequency
     * }
     */
    public static final OfDouble acoustic_frequencyLAYOUT() {
        return acoustic_frequencyLAYOUT;
    }

    private static final long acoustic_frequency$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double acoustic_frequency
     * }
     */
    public static final long acoustic_frequency$offset() {
        return acoustic_frequency$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double acoustic_frequency
     * }
     */
    public static double acoustic_frequency(MemorySegment struct) {
        return struct.get(acoustic_frequencyLAYOUT, acoustic_frequency$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double acoustic_frequency
     * }
     */
    public static void acoustic_frequency(MemorySegment struct, double fieldValue) {
        struct.set(acoustic_frequencyLAYOUT, acoustic_frequency$OFFSET, fieldValue);
    }

    private static final OfDouble sampling_frequencyLAYOUT = (OfDouble)LAYOUT.select(groupElement("sampling_frequency"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double sampling_frequency
     * }
     */
    public static final OfDouble sampling_frequencyLAYOUT() {
        return sampling_frequencyLAYOUT;
    }

    private static final long sampling_frequency$OFFSET = 24;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double sampling_frequency
     * }
     */
    public static final long sampling_frequency$offset() {
        return sampling_frequency$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double sampling_frequency
     * }
     */
    public static double sampling_frequency(MemorySegment struct) {
        return struct.get(sampling_frequencyLAYOUT, sampling_frequency$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double sampling_frequency
     * }
     */
    public static void sampling_frequency(MemorySegment struct, double fieldValue) {
        struct.set(sampling_frequencyLAYOUT, sampling_frequency$OFFSET, fieldValue);
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

    private static final long ping_number$OFFSET = 32;

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

    private static final OfInt num_samplesLAYOUT = (OfInt)LAYOUT.select(groupElement("num_samples"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned int num_samples
     * }
     */
    public static final OfInt num_samplesLAYOUT() {
        return num_samplesLAYOUT;
    }

    private static final long num_samples$OFFSET = 36;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned int num_samples
     * }
     */
    public static final long num_samples$offset() {
        return num_samples$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int num_samples
     * }
     */
    public static int num_samples(MemorySegment struct) {
        return struct.get(num_samplesLAYOUT, num_samples$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int num_samples
     * }
     */
    public static void num_samples(MemorySegment struct, int fieldValue) {
        struct.set(num_samplesLAYOUT, num_samples$OFFSET, fieldValue);
    }

    private static final OfInt num_raa_samplesLAYOUT = (OfInt)LAYOUT.select(groupElement("num_raa_samples"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned int num_raa_samples
     * }
     */
    public static final OfInt num_raa_samplesLAYOUT() {
        return num_raa_samplesLAYOUT;
    }

    private static final long num_raa_samples$OFFSET = 40;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned int num_raa_samples
     * }
     */
    public static final long num_raa_samples$offset() {
        return num_raa_samples$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int num_raa_samples
     * }
     */
    public static int num_raa_samples(MemorySegment struct) {
        return struct.get(num_raa_samplesLAYOUT, num_raa_samples$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int num_raa_samples
     * }
     */
    public static void num_raa_samples(MemorySegment struct, int fieldValue) {
        struct.set(num_raa_samplesLAYOUT, num_raa_samples$OFFSET, fieldValue);
    }

    private static final OfInt error_flagsLAYOUT = (OfInt)LAYOUT.select(groupElement("error_flags"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned int error_flags
     * }
     */
    public static final OfInt error_flagsLAYOUT() {
        return error_flagsLAYOUT;
    }

    private static final long error_flags$OFFSET = 44;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned int error_flags
     * }
     */
    public static final long error_flags$offset() {
        return error_flags$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int error_flags
     * }
     */
    public static int error_flags(MemorySegment struct) {
        return struct.get(error_flagsLAYOUT, error_flags$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int error_flags
     * }
     */
    public static void error_flags(MemorySegment struct, int fieldValue) {
        struct.set(error_flagsLAYOUT, error_flags$OFFSET, fieldValue);
    }

    private static final OfInt rangeLAYOUT = (OfInt)LAYOUT.select(groupElement("range"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned int range
     * }
     */
    public static final OfInt rangeLAYOUT() {
        return rangeLAYOUT;
    }

    private static final long range$OFFSET = 48;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned int range
     * }
     */
    public static final long range$offset() {
        return range$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int range
     * }
     */
    public static int range(MemorySegment struct) {
        return struct.get(rangeLAYOUT, range$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int range
     * }
     */
    public static void range(MemorySegment struct, int fieldValue) {
        struct.set(rangeLAYOUT, range$OFFSET, fieldValue);
    }

    private static final OfDouble fish_depthLAYOUT = (OfDouble)LAYOUT.select(groupElement("fish_depth"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double fish_depth
     * }
     */
    public static final OfDouble fish_depthLAYOUT() {
        return fish_depthLAYOUT;
    }

    private static final long fish_depth$OFFSET = 56;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double fish_depth
     * }
     */
    public static final long fish_depth$offset() {
        return fish_depth$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double fish_depth
     * }
     */
    public static double fish_depth(MemorySegment struct) {
        return struct.get(fish_depthLAYOUT, fish_depth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double fish_depth
     * }
     */
    public static void fish_depth(MemorySegment struct, double fieldValue) {
        struct.set(fish_depthLAYOUT, fish_depth$OFFSET, fieldValue);
    }

    private static final OfDouble fish_altitudeLAYOUT = (OfDouble)LAYOUT.select(groupElement("fish_altitude"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double fish_altitude
     * }
     */
    public static final OfDouble fish_altitudeLAYOUT() {
        return fish_altitudeLAYOUT;
    }

    private static final long fish_altitude$OFFSET = 64;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double fish_altitude
     * }
     */
    public static final long fish_altitude$offset() {
        return fish_altitude$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double fish_altitude
     * }
     */
    public static double fish_altitude(MemorySegment struct) {
        return struct.get(fish_altitudeLAYOUT, fish_altitude$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double fish_altitude
     * }
     */
    public static void fish_altitude(MemorySegment struct, double fieldValue) {
        struct.set(fish_altitudeLAYOUT, fish_altitude$OFFSET, fieldValue);
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

    private static final long sound_speed$OFFSET = 72;

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

    private static final OfInt tx_waveformLAYOUT = (OfInt)LAYOUT.select(groupElement("tx_waveform"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int tx_waveform
     * }
     */
    public static final OfInt tx_waveformLAYOUT() {
        return tx_waveformLAYOUT;
    }

    private static final long tx_waveform$OFFSET = 80;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int tx_waveform
     * }
     */
    public static final long tx_waveform$offset() {
        return tx_waveform$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int tx_waveform
     * }
     */
    public static int tx_waveform(MemorySegment struct) {
        return struct.get(tx_waveformLAYOUT, tx_waveform$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int tx_waveform
     * }
     */
    public static void tx_waveform(MemorySegment struct, int fieldValue) {
        struct.set(tx_waveformLAYOUT, tx_waveform$OFFSET, fieldValue);
    }

    private static final OfInt altimeterLAYOUT = (OfInt)LAYOUT.select(groupElement("altimeter"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int altimeter
     * }
     */
    public static final OfInt altimeterLAYOUT() {
        return altimeterLAYOUT;
    }

    private static final long altimeter$OFFSET = 84;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int altimeter
     * }
     */
    public static final long altimeter$offset() {
        return altimeter$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int altimeter
     * }
     */
    public static int altimeter(MemorySegment struct) {
        return struct.get(altimeterLAYOUT, altimeter$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int altimeter
     * }
     */
    public static void altimeter(MemorySegment struct, int fieldValue) {
        struct.set(altimeterLAYOUT, altimeter$OFFSET, fieldValue);
    }

    private static final OfInt raw_data_configLAYOUT = (OfInt)LAYOUT.select(groupElement("raw_data_config"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned int raw_data_config
     * }
     */
    public static final OfInt raw_data_configLAYOUT() {
        return raw_data_configLAYOUT;
    }

    private static final long raw_data_config$OFFSET = 88;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned int raw_data_config
     * }
     */
    public static final long raw_data_config$offset() {
        return raw_data_config$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int raw_data_config
     * }
     */
    public static int raw_data_config(MemorySegment struct) {
        return struct.get(raw_data_configLAYOUT, raw_data_config$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int raw_data_config
     * }
     */
    public static void raw_data_config(MemorySegment struct, int fieldValue) {
        struct.set(raw_data_configLAYOUT, raw_data_config$OFFSET, fieldValue);
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

    private static final long spare$OFFSET = 92;

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

