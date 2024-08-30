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
public final class SeamapSpecific extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        MemoryLayout.sequenceLayout(2, GSF.C_DOUBLE).withName("portTransmitter"),
        MemoryLayout.sequenceLayout(2, GSF.C_DOUBLE).withName("stbdTransmitter"),
        GSF.C_DOUBLE.withName("portGain"),
        GSF.C_DOUBLE.withName("stbdGain"),
        GSF.C_DOUBLE.withName("portPulseLength"),
        GSF.C_DOUBLE.withName("stbdPulseLength"),
        GSF.C_DOUBLE.withName("pressureDepth"),
        GSF.C_DOUBLE.withName("altitude"),
        GSF.C_DOUBLE.withName("temperature")
    ).withName("t_gsfSeamapSpecific");

    public SeamapSpecific(MemorySegment struct) {
        super(struct);
    }

    public SeamapSpecific(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final SequenceLayout portTransmitterLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("portTransmitter"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double portTransmitter[2]
     * }
     */
    public static final SequenceLayout portTransmitterLAYOUT() {
        return portTransmitterLAYOUT;
    }

    private static final long portTransmitter$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double portTransmitter[2]
     * }
     */
    public static final long portTransmitter$offset() {
        return portTransmitter$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double portTransmitter[2]
     * }
     */
    public static MemorySegment portTransmitter(MemorySegment struct) {
        return struct.asSlice(portTransmitter$OFFSET, portTransmitterLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double portTransmitter[2]
     * }
     */
    public static void portTransmitter(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, portTransmitter$OFFSET, portTransmitterLAYOUT.byteSize());
    }

    private static long[] portTransmitter$DIMS = { 2 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * double portTransmitter[2]
     * }
     */
    public static long[] portTransmitter$dimensions() {
        return portTransmitter$DIMS;
    }
    private static final VarHandle portTransmitter$ELEM_HANDLE = portTransmitterLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * double portTransmitter[2]
     * }
     */
    public static double portTransmitter(MemorySegment struct, long index0) {
        return (double)portTransmitter$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * double portTransmitter[2]
     * }
     */
    public static void portTransmitter(MemorySegment struct, long index0, double fieldValue) {
        portTransmitter$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final SequenceLayout stbdTransmitterLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("stbdTransmitter"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double stbdTransmitter[2]
     * }
     */
    public static final SequenceLayout stbdTransmitterLAYOUT() {
        return stbdTransmitterLAYOUT;
    }

    private static final long stbdTransmitter$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double stbdTransmitter[2]
     * }
     */
    public static final long stbdTransmitter$offset() {
        return stbdTransmitter$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double stbdTransmitter[2]
     * }
     */
    public static MemorySegment stbdTransmitter(MemorySegment struct) {
        return struct.asSlice(stbdTransmitter$OFFSET, stbdTransmitterLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double stbdTransmitter[2]
     * }
     */
    public static void stbdTransmitter(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, stbdTransmitter$OFFSET, stbdTransmitterLAYOUT.byteSize());
    }

    private static long[] stbdTransmitter$DIMS = { 2 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * double stbdTransmitter[2]
     * }
     */
    public static long[] stbdTransmitter$dimensions() {
        return stbdTransmitter$DIMS;
    }
    private static final VarHandle stbdTransmitter$ELEM_HANDLE = stbdTransmitterLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * double stbdTransmitter[2]
     * }
     */
    public static double stbdTransmitter(MemorySegment struct, long index0) {
        return (double)stbdTransmitter$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * double stbdTransmitter[2]
     * }
     */
    public static void stbdTransmitter(MemorySegment struct, long index0, double fieldValue) {
        stbdTransmitter$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final OfDouble portGainLAYOUT = (OfDouble)LAYOUT.select(groupElement("portGain"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double portGain
     * }
     */
    public static final OfDouble portGainLAYOUT() {
        return portGainLAYOUT;
    }

    private static final long portGain$OFFSET = 32;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double portGain
     * }
     */
    public static final long portGain$offset() {
        return portGain$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double portGain
     * }
     */
    public static double portGain(MemorySegment struct) {
        return struct.get(portGainLAYOUT, portGain$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double portGain
     * }
     */
    public static void portGain(MemorySegment struct, double fieldValue) {
        struct.set(portGainLAYOUT, portGain$OFFSET, fieldValue);
    }

    private static final OfDouble stbdGainLAYOUT = (OfDouble)LAYOUT.select(groupElement("stbdGain"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double stbdGain
     * }
     */
    public static final OfDouble stbdGainLAYOUT() {
        return stbdGainLAYOUT;
    }

    private static final long stbdGain$OFFSET = 40;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double stbdGain
     * }
     */
    public static final long stbdGain$offset() {
        return stbdGain$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double stbdGain
     * }
     */
    public static double stbdGain(MemorySegment struct) {
        return struct.get(stbdGainLAYOUT, stbdGain$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double stbdGain
     * }
     */
    public static void stbdGain(MemorySegment struct, double fieldValue) {
        struct.set(stbdGainLAYOUT, stbdGain$OFFSET, fieldValue);
    }

    private static final OfDouble portPulseLengthLAYOUT = (OfDouble)LAYOUT.select(groupElement("portPulseLength"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double portPulseLength
     * }
     */
    public static final OfDouble portPulseLengthLAYOUT() {
        return portPulseLengthLAYOUT;
    }

    private static final long portPulseLength$OFFSET = 48;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double portPulseLength
     * }
     */
    public static final long portPulseLength$offset() {
        return portPulseLength$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double portPulseLength
     * }
     */
    public static double portPulseLength(MemorySegment struct) {
        return struct.get(portPulseLengthLAYOUT, portPulseLength$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double portPulseLength
     * }
     */
    public static void portPulseLength(MemorySegment struct, double fieldValue) {
        struct.set(portPulseLengthLAYOUT, portPulseLength$OFFSET, fieldValue);
    }

    private static final OfDouble stbdPulseLengthLAYOUT = (OfDouble)LAYOUT.select(groupElement("stbdPulseLength"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double stbdPulseLength
     * }
     */
    public static final OfDouble stbdPulseLengthLAYOUT() {
        return stbdPulseLengthLAYOUT;
    }

    private static final long stbdPulseLength$OFFSET = 56;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double stbdPulseLength
     * }
     */
    public static final long stbdPulseLength$offset() {
        return stbdPulseLength$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double stbdPulseLength
     * }
     */
    public static double stbdPulseLength(MemorySegment struct) {
        return struct.get(stbdPulseLengthLAYOUT, stbdPulseLength$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double stbdPulseLength
     * }
     */
    public static void stbdPulseLength(MemorySegment struct, double fieldValue) {
        struct.set(stbdPulseLengthLAYOUT, stbdPulseLength$OFFSET, fieldValue);
    }

    private static final OfDouble pressureDepthLAYOUT = (OfDouble)LAYOUT.select(groupElement("pressureDepth"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double pressureDepth
     * }
     */
    public static final OfDouble pressureDepthLAYOUT() {
        return pressureDepthLAYOUT;
    }

    private static final long pressureDepth$OFFSET = 64;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double pressureDepth
     * }
     */
    public static final long pressureDepth$offset() {
        return pressureDepth$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double pressureDepth
     * }
     */
    public static double pressureDepth(MemorySegment struct) {
        return struct.get(pressureDepthLAYOUT, pressureDepth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double pressureDepth
     * }
     */
    public static void pressureDepth(MemorySegment struct, double fieldValue) {
        struct.set(pressureDepthLAYOUT, pressureDepth$OFFSET, fieldValue);
    }

    private static final OfDouble altitudeLAYOUT = (OfDouble)LAYOUT.select(groupElement("altitude"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double altitude
     * }
     */
    public static final OfDouble altitudeLAYOUT() {
        return altitudeLAYOUT;
    }

    private static final long altitude$OFFSET = 72;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double altitude
     * }
     */
    public static final long altitude$offset() {
        return altitude$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double altitude
     * }
     */
    public static double altitude(MemorySegment struct) {
        return struct.get(altitudeLAYOUT, altitude$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double altitude
     * }
     */
    public static void altitude(MemorySegment struct, double fieldValue) {
        struct.set(altitudeLAYOUT, altitude$OFFSET, fieldValue);
    }

    private static final OfDouble temperatureLAYOUT = (OfDouble)LAYOUT.select(groupElement("temperature"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double temperature
     * }
     */
    public static final OfDouble temperatureLAYOUT() {
        return temperatureLAYOUT;
    }

    private static final long temperature$OFFSET = 80;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double temperature
     * }
     */
    public static final long temperature$offset() {
        return temperature$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double temperature
     * }
     */
    public static double temperature(MemorySegment struct) {
        return struct.get(temperatureLAYOUT, temperature$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double temperature
     * }
     */
    public static void temperature(MemorySegment struct, double fieldValue) {
        struct.set(temperatureLAYOUT, temperature$OFFSET, fieldValue);
    }

}

