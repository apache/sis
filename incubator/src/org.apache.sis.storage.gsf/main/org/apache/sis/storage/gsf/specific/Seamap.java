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
public final class Seamap extends StructClass {

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

    public Seamap(MemorySegment struct) {
        super(struct);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final SequenceLayout portTransmitterLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("portTransmitter"));

    private static final long portTransmitter$OFFSET = 0;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double portTransmitter[2]
     * }
     */
    public MemorySegment portTransmitter() {
        return struct.asSlice(portTransmitter$OFFSET, portTransmitterLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double portTransmitter[2]
     * }
     */
    public void portTransmitter(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, portTransmitter$OFFSET, portTransmitterLAYOUT.byteSize());
    }

    private static final VarHandle portTransmitter$ELEM_HANDLE = portTransmitterLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * double portTransmitter[2]
     * }
     */
    public double portTransmitter(long index0) {
        return (double)portTransmitter$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * double portTransmitter[2]
     * }
     */
    public void portTransmitter(long index0, double fieldValue) {
        portTransmitter$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final SequenceLayout stbdTransmitterLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("stbdTransmitter"));

    private static final long stbdTransmitter$OFFSET = 16;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double stbdTransmitter[2]
     * }
     */
    public MemorySegment stbdTransmitter() {
        return struct.asSlice(stbdTransmitter$OFFSET, stbdTransmitterLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double stbdTransmitter[2]
     * }
     */
    public void stbdTransmitter(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, stbdTransmitter$OFFSET, stbdTransmitterLAYOUT.byteSize());
    }

    private static final VarHandle stbdTransmitter$ELEM_HANDLE = stbdTransmitterLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * double stbdTransmitter[2]
     * }
     */
    public double stbdTransmitter(long index0) {
        return (double)stbdTransmitter$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * double stbdTransmitter[2]
     * }
     */
    public void stbdTransmitter(long index0, double fieldValue) {
        stbdTransmitter$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final OfDouble portGainLAYOUT = (OfDouble)LAYOUT.select(groupElement("portGain"));

    private static final long portGain$OFFSET = 32;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double portGain
     * }
     */
    public double portGain() {
        return struct.get(portGainLAYOUT, portGain$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double portGain
     * }
     */
    public void portGain(double fieldValue) {
        struct.set(portGainLAYOUT, portGain$OFFSET, fieldValue);
    }

    private static final OfDouble stbdGainLAYOUT = (OfDouble)LAYOUT.select(groupElement("stbdGain"));

    private static final long stbdGain$OFFSET = 40;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double stbdGain
     * }
     */
    public double stbdGain() {
        return struct.get(stbdGainLAYOUT, stbdGain$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double stbdGain
     * }
     */
    public void stbdGain(double fieldValue) {
        struct.set(stbdGainLAYOUT, stbdGain$OFFSET, fieldValue);
    }

    private static final OfDouble portPulseLengthLAYOUT = (OfDouble)LAYOUT.select(groupElement("portPulseLength"));

    private static final long portPulseLength$OFFSET = 48;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double portPulseLength
     * }
     */
    public double portPulseLength() {
        return struct.get(portPulseLengthLAYOUT, portPulseLength$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double portPulseLength
     * }
     */
    public void portPulseLength(double fieldValue) {
        struct.set(portPulseLengthLAYOUT, portPulseLength$OFFSET, fieldValue);
    }

    private static final OfDouble stbdPulseLengthLAYOUT = (OfDouble)LAYOUT.select(groupElement("stbdPulseLength"));

    private static final long stbdPulseLength$OFFSET = 56;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double stbdPulseLength
     * }
     */
    public double stbdPulseLength() {
        return struct.get(stbdPulseLengthLAYOUT, stbdPulseLength$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double stbdPulseLength
     * }
     */
    public void stbdPulseLength(double fieldValue) {
        struct.set(stbdPulseLengthLAYOUT, stbdPulseLength$OFFSET, fieldValue);
    }

    private static final OfDouble pressureDepthLAYOUT = (OfDouble)LAYOUT.select(groupElement("pressureDepth"));

    private static final long pressureDepth$OFFSET = 64;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double pressureDepth
     * }
     */
    public double pressureDepth() {
        return struct.get(pressureDepthLAYOUT, pressureDepth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double pressureDepth
     * }
     */
    public void pressureDepth(double fieldValue) {
        struct.set(pressureDepthLAYOUT, pressureDepth$OFFSET, fieldValue);
    }

    private static final OfDouble altitudeLAYOUT = (OfDouble)LAYOUT.select(groupElement("altitude"));

    private static final long altitude$OFFSET = 72;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double altitude
     * }
     */
    public double altitude() {
        return struct.get(altitudeLAYOUT, altitude$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double altitude
     * }
     */
    public void altitude(double fieldValue) {
        struct.set(altitudeLAYOUT, altitude$OFFSET, fieldValue);
    }

    private static final OfDouble temperatureLAYOUT = (OfDouble)LAYOUT.select(groupElement("temperature"));

    private static final long temperature$OFFSET = 80;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double temperature
     * }
     */
    public double temperature() {
        return struct.get(temperatureLAYOUT, temperature$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double temperature
     * }
     */
    public void temperature(double fieldValue) {
        struct.set(temperatureLAYOUT, temperature$OFFSET, fieldValue);
    }
}
