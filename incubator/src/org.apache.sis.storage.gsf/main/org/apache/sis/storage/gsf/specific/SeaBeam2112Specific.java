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
public final class SeaBeam2112Specific extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("mode"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("surface_velocity"),
        GSF.C_CHAR.withName("ssv_source"),
        MemoryLayout.paddingLayout(3),
        GSF.C_INT.withName("ping_gain"),
        GSF.C_INT.withName("pulse_width"),
        GSF.C_INT.withName("transmitter_attenuation"),
        GSF.C_INT.withName("number_algorithms"),
        MemoryLayout.sequenceLayout(5, GSF.C_CHAR).withName("algorithm_order"),
        MemoryLayout.sequenceLayout(2, GSF.C_CHAR).withName("spare"),
        MemoryLayout.paddingLayout(5)
    ).withName("t_gsfSeaBeam2112Specific");

    public SeaBeam2112Specific(MemorySegment struct) {
        super(struct);
    }

    public SeaBeam2112Specific(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
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

    private static final long mode$OFFSET = 0;

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

    private static final OfDouble surface_velocityLAYOUT = (OfDouble)LAYOUT.select(groupElement("surface_velocity"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public static final OfDouble surface_velocityLAYOUT() {
        return surface_velocityLAYOUT;
    }

    private static final long surface_velocity$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public static final long surface_velocity$offset() {
        return surface_velocity$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public static double surface_velocity(MemorySegment struct) {
        return struct.get(surface_velocityLAYOUT, surface_velocity$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public static void surface_velocity(MemorySegment struct, double fieldValue) {
        struct.set(surface_velocityLAYOUT, surface_velocity$OFFSET, fieldValue);
    }

    private static final OfByte ssv_sourceLAYOUT = (OfByte)LAYOUT.select(groupElement("ssv_source"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * char ssv_source
     * }
     */
    public static final OfByte ssv_sourceLAYOUT() {
        return ssv_sourceLAYOUT;
    }

    private static final long ssv_source$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * char ssv_source
     * }
     */
    public static final long ssv_source$offset() {
        return ssv_source$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char ssv_source
     * }
     */
    public static byte ssv_source(MemorySegment struct) {
        return struct.get(ssv_sourceLAYOUT, ssv_source$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char ssv_source
     * }
     */
    public static void ssv_source(MemorySegment struct, byte fieldValue) {
        struct.set(ssv_sourceLAYOUT, ssv_source$OFFSET, fieldValue);
    }

    private static final OfInt ping_gainLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_gain"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int ping_gain
     * }
     */
    public static final OfInt ping_gainLAYOUT() {
        return ping_gainLAYOUT;
    }

    private static final long ping_gain$OFFSET = 20;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int ping_gain
     * }
     */
    public static final long ping_gain$offset() {
        return ping_gain$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_gain
     * }
     */
    public static int ping_gain(MemorySegment struct) {
        return struct.get(ping_gainLAYOUT, ping_gain$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_gain
     * }
     */
    public static void ping_gain(MemorySegment struct, int fieldValue) {
        struct.set(ping_gainLAYOUT, ping_gain$OFFSET, fieldValue);
    }

    private static final OfInt pulse_widthLAYOUT = (OfInt)LAYOUT.select(groupElement("pulse_width"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int pulse_width
     * }
     */
    public static final OfInt pulse_widthLAYOUT() {
        return pulse_widthLAYOUT;
    }

    private static final long pulse_width$OFFSET = 24;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int pulse_width
     * }
     */
    public static final long pulse_width$offset() {
        return pulse_width$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int pulse_width
     * }
     */
    public static int pulse_width(MemorySegment struct) {
        return struct.get(pulse_widthLAYOUT, pulse_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int pulse_width
     * }
     */
    public static void pulse_width(MemorySegment struct, int fieldValue) {
        struct.set(pulse_widthLAYOUT, pulse_width$OFFSET, fieldValue);
    }

    private static final OfInt transmitter_attenuationLAYOUT = (OfInt)LAYOUT.select(groupElement("transmitter_attenuation"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int transmitter_attenuation
     * }
     */
    public static final OfInt transmitter_attenuationLAYOUT() {
        return transmitter_attenuationLAYOUT;
    }

    private static final long transmitter_attenuation$OFFSET = 28;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int transmitter_attenuation
     * }
     */
    public static final long transmitter_attenuation$offset() {
        return transmitter_attenuation$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int transmitter_attenuation
     * }
     */
    public static int transmitter_attenuation(MemorySegment struct) {
        return struct.get(transmitter_attenuationLAYOUT, transmitter_attenuation$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int transmitter_attenuation
     * }
     */
    public static void transmitter_attenuation(MemorySegment struct, int fieldValue) {
        struct.set(transmitter_attenuationLAYOUT, transmitter_attenuation$OFFSET, fieldValue);
    }

    private static final OfInt number_algorithmsLAYOUT = (OfInt)LAYOUT.select(groupElement("number_algorithms"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int number_algorithms
     * }
     */
    public static final OfInt number_algorithmsLAYOUT() {
        return number_algorithmsLAYOUT;
    }

    private static final long number_algorithms$OFFSET = 32;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int number_algorithms
     * }
     */
    public static final long number_algorithms$offset() {
        return number_algorithms$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int number_algorithms
     * }
     */
    public static int number_algorithms(MemorySegment struct) {
        return struct.get(number_algorithmsLAYOUT, number_algorithms$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int number_algorithms
     * }
     */
    public static void number_algorithms(MemorySegment struct, int fieldValue) {
        struct.set(number_algorithmsLAYOUT, number_algorithms$OFFSET, fieldValue);
    }

    private static final SequenceLayout algorithm_orderLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("algorithm_order"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * char algorithm_order[5]
     * }
     */
    public static final SequenceLayout algorithm_orderLAYOUT() {
        return algorithm_orderLAYOUT;
    }

    private static final long algorithm_order$OFFSET = 36;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * char algorithm_order[5]
     * }
     */
    public static final long algorithm_order$offset() {
        return algorithm_order$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char algorithm_order[5]
     * }
     */
    public static MemorySegment algorithm_order(MemorySegment struct) {
        return struct.asSlice(algorithm_order$OFFSET, algorithm_orderLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char algorithm_order[5]
     * }
     */
    public static void algorithm_order(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, algorithm_order$OFFSET, algorithm_orderLAYOUT.byteSize());
    }

    private static long[] algorithm_order$DIMS = { 5 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * char algorithm_order[5]
     * }
     */
    public static long[] algorithm_order$dimensions() {
        return algorithm_order$DIMS;
    }
    private static final VarHandle algorithm_order$ELEM_HANDLE = algorithm_orderLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * char algorithm_order[5]
     * }
     */
    public static byte algorithm_order(MemorySegment struct, long index0) {
        return (byte)algorithm_order$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * char algorithm_order[5]
     * }
     */
    public static void algorithm_order(MemorySegment struct, long index0, byte fieldValue) {
        algorithm_order$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public static final SequenceLayout spareLAYOUT() {
        return spareLAYOUT;
    }

    private static final long spare$OFFSET = 41;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public static final long spare$offset() {
        return spare$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public static MemorySegment spare(MemorySegment struct) {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public static void spare(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static long[] spare$DIMS = { 2 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public static long[] spare$dimensions() {
        return spare$DIMS;
    }
    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public static byte spare(MemorySegment struct, long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public static void spare(MemorySegment struct, long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

}

