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

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt modeLAYOUT = (OfInt)LAYOUT.select(groupElement("mode"));

    private static final long mode$OFFSET = 0;

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

    private static final OfDouble surface_velocityLAYOUT = (OfDouble)LAYOUT.select(groupElement("surface_velocity"));

    private static final long surface_velocity$OFFSET = 8;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public double surface_velocity() {
        return struct.get(surface_velocityLAYOUT, surface_velocity$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public void surface_velocity(double fieldValue) {
        struct.set(surface_velocityLAYOUT, surface_velocity$OFFSET, fieldValue);
    }

    private static final OfByte ssv_sourceLAYOUT = (OfByte)LAYOUT.select(groupElement("ssv_source"));

    private static final long ssv_source$OFFSET = 16;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char ssv_source
     * }
     */
    public byte ssv_source() {
        return struct.get(ssv_sourceLAYOUT, ssv_source$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char ssv_source
     * }
     */
    public void ssv_source(byte fieldValue) {
        struct.set(ssv_sourceLAYOUT, ssv_source$OFFSET, fieldValue);
    }

    private static final OfInt ping_gainLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_gain"));

    private static final long ping_gain$OFFSET = 20;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_gain
     * }
     */
    public int ping_gain() {
        return struct.get(ping_gainLAYOUT, ping_gain$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_gain
     * }
     */
    public void ping_gain(int fieldValue) {
        struct.set(ping_gainLAYOUT, ping_gain$OFFSET, fieldValue);
    }

    private static final OfInt pulse_widthLAYOUT = (OfInt)LAYOUT.select(groupElement("pulse_width"));

    private static final long pulse_width$OFFSET = 24;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int pulse_width
     * }
     */
    public int pulse_width() {
        return struct.get(pulse_widthLAYOUT, pulse_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int pulse_width
     * }
     */
    public void pulse_width(int fieldValue) {
        struct.set(pulse_widthLAYOUT, pulse_width$OFFSET, fieldValue);
    }

    private static final OfInt transmitter_attenuationLAYOUT = (OfInt)LAYOUT.select(groupElement("transmitter_attenuation"));

    private static final long transmitter_attenuation$OFFSET = 28;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int transmitter_attenuation
     * }
     */
    public int transmitter_attenuation() {
        return struct.get(transmitter_attenuationLAYOUT, transmitter_attenuation$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int transmitter_attenuation
     * }
     */
    public void transmitter_attenuation(int fieldValue) {
        struct.set(transmitter_attenuationLAYOUT, transmitter_attenuation$OFFSET, fieldValue);
    }

    private static final OfInt number_algorithmsLAYOUT = (OfInt)LAYOUT.select(groupElement("number_algorithms"));

    private static final long number_algorithms$OFFSET = 32;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int number_algorithms
     * }
     */
    public int number_algorithms() {
        return struct.get(number_algorithmsLAYOUT, number_algorithms$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int number_algorithms
     * }
     */
    public void number_algorithms(int fieldValue) {
        struct.set(number_algorithmsLAYOUT, number_algorithms$OFFSET, fieldValue);
    }

    private static final SequenceLayout algorithm_orderLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("algorithm_order"));

    private static final long algorithm_order$OFFSET = 36;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char algorithm_order[5]
     * }
     */
    public MemorySegment algorithm_order() {
        return struct.asSlice(algorithm_order$OFFSET, algorithm_orderLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char algorithm_order[5]
     * }
     */
    public void algorithm_order(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, algorithm_order$OFFSET, algorithm_orderLAYOUT.byteSize());
    }

    private static final VarHandle algorithm_order$ELEM_HANDLE = algorithm_orderLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * char algorithm_order[5]
     * }
     */
    public byte algorithm_order(long index0) {
        return (byte)algorithm_order$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * char algorithm_order[5]
     * }
     */
    public void algorithm_order(long index0, byte fieldValue) {
        algorithm_order$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    private static final long spare$OFFSET = 41;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public MemorySegment spare() {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public void spare(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public byte spare(long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * char spare[2]
     * }
     */
    public void spare(long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }
}
