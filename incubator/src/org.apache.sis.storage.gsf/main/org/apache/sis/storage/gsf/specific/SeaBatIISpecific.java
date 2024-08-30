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
public final class SeaBatIISpecific extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("ping_number"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("surface_velocity"),
        GSF.C_INT.withName("mode"),
        GSF.C_INT.withName("sonar_range"),
        GSF.C_INT.withName("transmit_power"),
        GSF.C_INT.withName("receive_gain"),
        GSF.C_DOUBLE.withName("fore_aft_bw"),
        GSF.C_DOUBLE.withName("athwart_bw"),
        MemoryLayout.sequenceLayout(4, GSF.C_CHAR).withName("spare"),
        MemoryLayout.paddingLayout(4)
    ).withName("t_gsfSeaBatIISpecific");

    public SeaBatIISpecific(MemorySegment struct) {
        super(struct);
    }

    public SeaBatIISpecific(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
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

    private static final long ping_number$OFFSET = 0;

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

    private static final long mode$OFFSET = 16;

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

    private static final OfInt sonar_rangeLAYOUT = (OfInt)LAYOUT.select(groupElement("sonar_range"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int sonar_range
     * }
     */
    public static final OfInt sonar_rangeLAYOUT() {
        return sonar_rangeLAYOUT;
    }

    private static final long sonar_range$OFFSET = 20;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int sonar_range
     * }
     */
    public static final long sonar_range$offset() {
        return sonar_range$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int sonar_range
     * }
     */
    public static int sonar_range(MemorySegment struct) {
        return struct.get(sonar_rangeLAYOUT, sonar_range$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int sonar_range
     * }
     */
    public static void sonar_range(MemorySegment struct, int fieldValue) {
        struct.set(sonar_rangeLAYOUT, sonar_range$OFFSET, fieldValue);
    }

    private static final OfInt transmit_powerLAYOUT = (OfInt)LAYOUT.select(groupElement("transmit_power"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int transmit_power
     * }
     */
    public static final OfInt transmit_powerLAYOUT() {
        return transmit_powerLAYOUT;
    }

    private static final long transmit_power$OFFSET = 24;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int transmit_power
     * }
     */
    public static final long transmit_power$offset() {
        return transmit_power$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int transmit_power
     * }
     */
    public static int transmit_power(MemorySegment struct) {
        return struct.get(transmit_powerLAYOUT, transmit_power$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int transmit_power
     * }
     */
    public static void transmit_power(MemorySegment struct, int fieldValue) {
        struct.set(transmit_powerLAYOUT, transmit_power$OFFSET, fieldValue);
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

    private static final long receive_gain$OFFSET = 28;

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

    private static final OfDouble fore_aft_bwLAYOUT = (OfDouble)LAYOUT.select(groupElement("fore_aft_bw"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double fore_aft_bw
     * }
     */
    public static final OfDouble fore_aft_bwLAYOUT() {
        return fore_aft_bwLAYOUT;
    }

    private static final long fore_aft_bw$OFFSET = 32;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double fore_aft_bw
     * }
     */
    public static final long fore_aft_bw$offset() {
        return fore_aft_bw$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double fore_aft_bw
     * }
     */
    public static double fore_aft_bw(MemorySegment struct) {
        return struct.get(fore_aft_bwLAYOUT, fore_aft_bw$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double fore_aft_bw
     * }
     */
    public static void fore_aft_bw(MemorySegment struct, double fieldValue) {
        struct.set(fore_aft_bwLAYOUT, fore_aft_bw$OFFSET, fieldValue);
    }

    private static final OfDouble athwart_bwLAYOUT = (OfDouble)LAYOUT.select(groupElement("athwart_bw"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double athwart_bw
     * }
     */
    public static final OfDouble athwart_bwLAYOUT() {
        return athwart_bwLAYOUT;
    }

    private static final long athwart_bw$OFFSET = 40;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double athwart_bw
     * }
     */
    public static final long athwart_bw$offset() {
        return athwart_bw$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double athwart_bw
     * }
     */
    public static double athwart_bw(MemorySegment struct) {
        return struct.get(athwart_bwLAYOUT, athwart_bw$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double athwart_bw
     * }
     */
    public static void athwart_bw(MemorySegment struct, double fieldValue) {
        struct.set(athwart_bwLAYOUT, athwart_bw$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public static final SequenceLayout spareLAYOUT() {
        return spareLAYOUT;
    }

    private static final long spare$OFFSET = 48;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public static final long spare$offset() {
        return spare$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public static MemorySegment spare(MemorySegment struct) {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public static void spare(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static long[] spare$DIMS = { 4 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public static long[] spare$dimensions() {
        return spare$DIMS;
    }
    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public static byte spare(MemorySegment struct, long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public static void spare(MemorySegment struct, long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

}

