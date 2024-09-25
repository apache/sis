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

    private static final OfInt ping_numLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_num"));

    private static final long ping_num$OFFSET = 4;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_num
     * }
     */
    public int ping_num() {
        return struct.get(ping_numLAYOUT, ping_num$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_num
     * }
     */
    public void ping_num(int fieldValue) {
        struct.set(ping_numLAYOUT, ping_num$OFFSET, fieldValue);
    }

    private static final OfInt sound_velLAYOUT = (OfInt)LAYOUT.select(groupElement("sound_vel"));

    private static final long sound_vel$OFFSET = 8;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int sound_vel
     * }
     */
    public int sound_vel() {
        return struct.get(sound_velLAYOUT, sound_vel$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int sound_vel
     * }
     */
    public void sound_vel(int fieldValue) {
        struct.set(sound_velLAYOUT, sound_vel$OFFSET, fieldValue);
    }

    private static final OfInt pulse_lengthLAYOUT = (OfInt)LAYOUT.select(groupElement("pulse_length"));

    private static final long pulse_length$OFFSET = 12;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int pulse_length
     * }
     */
    public int pulse_length() {
        return struct.get(pulse_lengthLAYOUT, pulse_length$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int pulse_length
     * }
     */
    public void pulse_length(int fieldValue) {
        struct.set(pulse_lengthLAYOUT, pulse_length$OFFSET, fieldValue);
    }

    private static final OfInt receiver_gain_stbdLAYOUT = (OfInt)LAYOUT.select(groupElement("receiver_gain_stbd"));

    private static final long receiver_gain_stbd$OFFSET = 16;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int receiver_gain_stbd
     * }
     */
    public int receiver_gain_stbd() {
        return struct.get(receiver_gain_stbdLAYOUT, receiver_gain_stbd$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int receiver_gain_stbd
     * }
     */
    public void receiver_gain_stbd(int fieldValue) {
        struct.set(receiver_gain_stbdLAYOUT, receiver_gain_stbd$OFFSET, fieldValue);
    }

    private static final OfInt receiver_gain_portLAYOUT = (OfInt)LAYOUT.select(groupElement("receiver_gain_port"));

    private static final long receiver_gain_port$OFFSET = 20;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int receiver_gain_port
     * }
     */
    public int receiver_gain_port() {
        return struct.get(receiver_gain_portLAYOUT, receiver_gain_port$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int receiver_gain_port
     * }
     */
    public void receiver_gain_port(int fieldValue) {
        struct.set(receiver_gain_portLAYOUT, receiver_gain_port$OFFSET, fieldValue);
    }

    private static final OfInt reservedLAYOUT = (OfInt)LAYOUT.select(groupElement("reserved"));

    private static final long reserved$OFFSET = 24;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int reserved
     * }
     */
    public int reserved() {
        return struct.get(reservedLAYOUT, reserved$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int reserved
     * }
     */
    public void reserved(int fieldValue) {
        struct.set(reservedLAYOUT, reserved$OFFSET, fieldValue);
    }
}
