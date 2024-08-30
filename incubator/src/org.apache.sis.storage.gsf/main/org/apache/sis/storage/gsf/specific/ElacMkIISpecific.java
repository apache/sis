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

import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.MemoryLayout.PathElement.*;
import org.apache.sis.storage.gsf.GSF;
import org.apache.sis.storage.gsf.StructClass;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ElacMkIISpecific extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("mode"),
        GSF.C_INT.withName("ping_num"),
        GSF.C_INT.withName("sound_vel"),
        GSF.C_INT.withName("pulse_length"),
        GSF.C_INT.withName("receiver_gain_stbd"),
        GSF.C_INT.withName("receiver_gain_port"),
        GSF.C_INT.withName("reserved")
    ).withName("t_gsfElacMkIISpecific");

    public ElacMkIISpecific(MemorySegment struct) {
        super(struct);
    }

    public ElacMkIISpecific(SegmentAllocator allocator) {
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

    private static final OfInt ping_numLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_num"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int ping_num
     * }
     */
    public static final OfInt ping_numLAYOUT() {
        return ping_numLAYOUT;
    }

    private static final long ping_num$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int ping_num
     * }
     */
    public static final long ping_num$offset() {
        return ping_num$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_num
     * }
     */
    public static int ping_num(MemorySegment struct) {
        return struct.get(ping_numLAYOUT, ping_num$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_num
     * }
     */
    public static void ping_num(MemorySegment struct, int fieldValue) {
        struct.set(ping_numLAYOUT, ping_num$OFFSET, fieldValue);
    }

    private static final OfInt sound_velLAYOUT = (OfInt)LAYOUT.select(groupElement("sound_vel"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int sound_vel
     * }
     */
    public static final OfInt sound_velLAYOUT() {
        return sound_velLAYOUT;
    }

    private static final long sound_vel$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int sound_vel
     * }
     */
    public static final long sound_vel$offset() {
        return sound_vel$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int sound_vel
     * }
     */
    public static int sound_vel(MemorySegment struct) {
        return struct.get(sound_velLAYOUT, sound_vel$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int sound_vel
     * }
     */
    public static void sound_vel(MemorySegment struct, int fieldValue) {
        struct.set(sound_velLAYOUT, sound_vel$OFFSET, fieldValue);
    }

    private static final OfInt pulse_lengthLAYOUT = (OfInt)LAYOUT.select(groupElement("pulse_length"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int pulse_length
     * }
     */
    public static final OfInt pulse_lengthLAYOUT() {
        return pulse_lengthLAYOUT;
    }

    private static final long pulse_length$OFFSET = 12;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int pulse_length
     * }
     */
    public static final long pulse_length$offset() {
        return pulse_length$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int pulse_length
     * }
     */
    public static int pulse_length(MemorySegment struct) {
        return struct.get(pulse_lengthLAYOUT, pulse_length$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int pulse_length
     * }
     */
    public static void pulse_length(MemorySegment struct, int fieldValue) {
        struct.set(pulse_lengthLAYOUT, pulse_length$OFFSET, fieldValue);
    }

    private static final OfInt receiver_gain_stbdLAYOUT = (OfInt)LAYOUT.select(groupElement("receiver_gain_stbd"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int receiver_gain_stbd
     * }
     */
    public static final OfInt receiver_gain_stbdLAYOUT() {
        return receiver_gain_stbdLAYOUT;
    }

    private static final long receiver_gain_stbd$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int receiver_gain_stbd
     * }
     */
    public static final long receiver_gain_stbd$offset() {
        return receiver_gain_stbd$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int receiver_gain_stbd
     * }
     */
    public static int receiver_gain_stbd(MemorySegment struct) {
        return struct.get(receiver_gain_stbdLAYOUT, receiver_gain_stbd$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int receiver_gain_stbd
     * }
     */
    public static void receiver_gain_stbd(MemorySegment struct, int fieldValue) {
        struct.set(receiver_gain_stbdLAYOUT, receiver_gain_stbd$OFFSET, fieldValue);
    }

    private static final OfInt receiver_gain_portLAYOUT = (OfInt)LAYOUT.select(groupElement("receiver_gain_port"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int receiver_gain_port
     * }
     */
    public static final OfInt receiver_gain_portLAYOUT() {
        return receiver_gain_portLAYOUT;
    }

    private static final long receiver_gain_port$OFFSET = 20;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int receiver_gain_port
     * }
     */
    public static final long receiver_gain_port$offset() {
        return receiver_gain_port$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int receiver_gain_port
     * }
     */
    public static int receiver_gain_port(MemorySegment struct) {
        return struct.get(receiver_gain_portLAYOUT, receiver_gain_port$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int receiver_gain_port
     * }
     */
    public static void receiver_gain_port(MemorySegment struct, int fieldValue) {
        struct.set(receiver_gain_portLAYOUT, receiver_gain_port$OFFSET, fieldValue);
    }

    private static final OfInt reservedLAYOUT = (OfInt)LAYOUT.select(groupElement("reserved"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int reserved
     * }
     */
    public static final OfInt reservedLAYOUT() {
        return reservedLAYOUT;
    }

    private static final long reserved$OFFSET = 24;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int reserved
     * }
     */
    public static final long reserved$offset() {
        return reserved$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int reserved
     * }
     */
    public static int reserved(MemorySegment struct) {
        return struct.get(reservedLAYOUT, reserved$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int reserved
     * }
     */
    public static void reserved(MemorySegment struct, int fieldValue) {
        struct.set(reservedLAYOUT, reserved$OFFSET, fieldValue);
    }

}

