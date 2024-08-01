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
public final class EM3RawTxSector extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_DOUBLE.withName("tilt_angle"),
        GSF.C_DOUBLE.withName("focus_range"),
        GSF.C_DOUBLE.withName("signal_length"),
        GSF.C_DOUBLE.withName("transmit_delay"),
        GSF.C_DOUBLE.withName("center_frequency"),
        GSF.C_INT.withName("waveform_id"),
        GSF.C_INT.withName("sector_number"),
        GSF.C_DOUBLE.withName("signal_bandwidth"),
        MemoryLayout.sequenceLayout(16, GSF.C_CHAR).withName("spare")
    ).withName("t_gsfEM3RawTxSector");

    public EM3RawTxSector(MemorySegment struct) {
        super(struct);
    }

    public EM3RawTxSector(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfDouble tilt_angleLAYOUT = (OfDouble)LAYOUT.select(groupElement("tilt_angle"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double tilt_angle
     * }
     */
    public static final OfDouble tilt_angleLAYOUT() {
        return tilt_angleLAYOUT;
    }

    private static final long tilt_angle$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double tilt_angle
     * }
     */
    public static final long tilt_angle$offset() {
        return tilt_angle$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double tilt_angle
     * }
     */
    public static double tilt_angle(MemorySegment struct) {
        return struct.get(tilt_angleLAYOUT, tilt_angle$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double tilt_angle
     * }
     */
    public static void tilt_angle(MemorySegment struct, double fieldValue) {
        struct.set(tilt_angleLAYOUT, tilt_angle$OFFSET, fieldValue);
    }

    private static final OfDouble focus_rangeLAYOUT = (OfDouble)LAYOUT.select(groupElement("focus_range"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double focus_range
     * }
     */
    public static final OfDouble focus_rangeLAYOUT() {
        return focus_rangeLAYOUT;
    }

    private static final long focus_range$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double focus_range
     * }
     */
    public static final long focus_range$offset() {
        return focus_range$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double focus_range
     * }
     */
    public static double focus_range(MemorySegment struct) {
        return struct.get(focus_rangeLAYOUT, focus_range$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double focus_range
     * }
     */
    public static void focus_range(MemorySegment struct, double fieldValue) {
        struct.set(focus_rangeLAYOUT, focus_range$OFFSET, fieldValue);
    }

    private static final OfDouble signal_lengthLAYOUT = (OfDouble)LAYOUT.select(groupElement("signal_length"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double signal_length
     * }
     */
    public static final OfDouble signal_lengthLAYOUT() {
        return signal_lengthLAYOUT;
    }

    private static final long signal_length$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double signal_length
     * }
     */
    public static final long signal_length$offset() {
        return signal_length$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double signal_length
     * }
     */
    public static double signal_length(MemorySegment struct) {
        return struct.get(signal_lengthLAYOUT, signal_length$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double signal_length
     * }
     */
    public static void signal_length(MemorySegment struct, double fieldValue) {
        struct.set(signal_lengthLAYOUT, signal_length$OFFSET, fieldValue);
    }

    private static final OfDouble transmit_delayLAYOUT = (OfDouble)LAYOUT.select(groupElement("transmit_delay"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double transmit_delay
     * }
     */
    public static final OfDouble transmit_delayLAYOUT() {
        return transmit_delayLAYOUT;
    }

    private static final long transmit_delay$OFFSET = 24;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double transmit_delay
     * }
     */
    public static final long transmit_delay$offset() {
        return transmit_delay$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double transmit_delay
     * }
     */
    public static double transmit_delay(MemorySegment struct) {
        return struct.get(transmit_delayLAYOUT, transmit_delay$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double transmit_delay
     * }
     */
    public static void transmit_delay(MemorySegment struct, double fieldValue) {
        struct.set(transmit_delayLAYOUT, transmit_delay$OFFSET, fieldValue);
    }

    private static final OfDouble center_frequencyLAYOUT = (OfDouble)LAYOUT.select(groupElement("center_frequency"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double center_frequency
     * }
     */
    public static final OfDouble center_frequencyLAYOUT() {
        return center_frequencyLAYOUT;
    }

    private static final long center_frequency$OFFSET = 32;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double center_frequency
     * }
     */
    public static final long center_frequency$offset() {
        return center_frequency$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double center_frequency
     * }
     */
    public static double center_frequency(MemorySegment struct) {
        return struct.get(center_frequencyLAYOUT, center_frequency$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double center_frequency
     * }
     */
    public static void center_frequency(MemorySegment struct, double fieldValue) {
        struct.set(center_frequencyLAYOUT, center_frequency$OFFSET, fieldValue);
    }

    private static final OfInt waveform_idLAYOUT = (OfInt)LAYOUT.select(groupElement("waveform_id"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int waveform_id
     * }
     */
    public static final OfInt waveform_idLAYOUT() {
        return waveform_idLAYOUT;
    }

    private static final long waveform_id$OFFSET = 40;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int waveform_id
     * }
     */
    public static final long waveform_id$offset() {
        return waveform_id$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int waveform_id
     * }
     */
    public static int waveform_id(MemorySegment struct) {
        return struct.get(waveform_idLAYOUT, waveform_id$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int waveform_id
     * }
     */
    public static void waveform_id(MemorySegment struct, int fieldValue) {
        struct.set(waveform_idLAYOUT, waveform_id$OFFSET, fieldValue);
    }

    private static final OfInt sector_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("sector_number"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int sector_number
     * }
     */
    public static final OfInt sector_numberLAYOUT() {
        return sector_numberLAYOUT;
    }

    private static final long sector_number$OFFSET = 44;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int sector_number
     * }
     */
    public static final long sector_number$offset() {
        return sector_number$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int sector_number
     * }
     */
    public static int sector_number(MemorySegment struct) {
        return struct.get(sector_numberLAYOUT, sector_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int sector_number
     * }
     */
    public static void sector_number(MemorySegment struct, int fieldValue) {
        struct.set(sector_numberLAYOUT, sector_number$OFFSET, fieldValue);
    }

    private static final OfDouble signal_bandwidthLAYOUT = (OfDouble)LAYOUT.select(groupElement("signal_bandwidth"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double signal_bandwidth
     * }
     */
    public static final OfDouble signal_bandwidthLAYOUT() {
        return signal_bandwidthLAYOUT;
    }

    private static final long signal_bandwidth$OFFSET = 48;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double signal_bandwidth
     * }
     */
    public static final long signal_bandwidth$offset() {
        return signal_bandwidth$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double signal_bandwidth
     * }
     */
    public static double signal_bandwidth(MemorySegment struct) {
        return struct.get(signal_bandwidthLAYOUT, signal_bandwidth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double signal_bandwidth
     * }
     */
    public static void signal_bandwidth(MemorySegment struct, double fieldValue) {
        struct.set(signal_bandwidthLAYOUT, signal_bandwidth$OFFSET, fieldValue);
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

    private static final long spare$OFFSET = 56;

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

