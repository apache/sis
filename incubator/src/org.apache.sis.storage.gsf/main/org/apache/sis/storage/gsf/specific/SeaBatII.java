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
public final class SeaBatII extends StructClass {
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

    public SeaBatII(MemorySegment struct) {
        super(struct);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt ping_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_number"));

    private static final long ping_number$OFFSET = 0;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_number
     * }
     */
    public int ping_number() {
        return struct.get(ping_numberLAYOUT, ping_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_number
     * }
     */
    public void ping_number(int fieldValue) {
        struct.set(ping_numberLAYOUT, ping_number$OFFSET, fieldValue);
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

    private static final OfInt modeLAYOUT = (OfInt)LAYOUT.select(groupElement("mode"));

    private static final long mode$OFFSET = 16;

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

    private static final OfInt sonar_rangeLAYOUT = (OfInt)LAYOUT.select(groupElement("sonar_range"));

    private static final long sonar_range$OFFSET = 20;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int sonar_range
     * }
     */
    public int sonar_range() {
        return struct.get(sonar_rangeLAYOUT, sonar_range$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int sonar_range
     * }
     */
    public void sonar_range(int fieldValue) {
        struct.set(sonar_rangeLAYOUT, sonar_range$OFFSET, fieldValue);
    }

    private static final OfInt transmit_powerLAYOUT = (OfInt)LAYOUT.select(groupElement("transmit_power"));

    private static final long transmit_power$OFFSET = 24;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int transmit_power
     * }
     */
    public int transmit_power() {
        return struct.get(transmit_powerLAYOUT, transmit_power$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int transmit_power
     * }
     */
    public void transmit_power(int fieldValue) {
        struct.set(transmit_powerLAYOUT, transmit_power$OFFSET, fieldValue);
    }

    private static final OfInt receive_gainLAYOUT = (OfInt)LAYOUT.select(groupElement("receive_gain"));

    private static final long receive_gain$OFFSET = 28;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int receive_gain
     * }
     */
    public int receive_gain() {
        return struct.get(receive_gainLAYOUT, receive_gain$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int receive_gain
     * }
     */
    public void receive_gain(int fieldValue) {
        struct.set(receive_gainLAYOUT, receive_gain$OFFSET, fieldValue);
    }

    private static final OfDouble fore_aft_bwLAYOUT = (OfDouble)LAYOUT.select(groupElement("fore_aft_bw"));

    private static final long fore_aft_bw$OFFSET = 32;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double fore_aft_bw
     * }
     */
    public double fore_aft_bw() {
        return struct.get(fore_aft_bwLAYOUT, fore_aft_bw$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double fore_aft_bw
     * }
     */
    public void fore_aft_bw(double fieldValue) {
        struct.set(fore_aft_bwLAYOUT, fore_aft_bw$OFFSET, fieldValue);
    }

    private static final OfDouble athwart_bwLAYOUT = (OfDouble)LAYOUT.select(groupElement("athwart_bw"));

    private static final long athwart_bw$OFFSET = 40;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double athwart_bw
     * }
     */
    public double athwart_bw() {
        return struct.get(athwart_bwLAYOUT, athwart_bw$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double athwart_bw
     * }
     */
    public void athwart_bw(double fieldValue) {
        struct.set(athwart_bwLAYOUT, athwart_bw$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    private static final long spare$OFFSET = 48;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public MemorySegment spare() {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public void spare(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public byte spare(long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public void spare(long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }
}
