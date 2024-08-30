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
public final class TypeIIISpecific extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_SHORT.withName("leftmost_beam"),
        GSF.C_SHORT.withName("rightmost_beam"),
        GSF.C_SHORT.withName("total_beams"),
        GSF.C_SHORT.withName("nav_mode"),
        GSF.C_SHORT.withName("ping_number"),
        GSF.C_SHORT.withName("mission_number")
    ).withName("t_gsfTypeIIISpecific");

    public TypeIIISpecific(MemorySegment struct) {
        super(struct);
    }

    public TypeIIISpecific(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfShort leftmost_beamLAYOUT = (OfShort)LAYOUT.select(groupElement("leftmost_beam"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned short leftmost_beam
     * }
     */
    public static final OfShort leftmost_beamLAYOUT() {
        return leftmost_beamLAYOUT;
    }

    private static final long leftmost_beam$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned short leftmost_beam
     * }
     */
    public static final long leftmost_beam$offset() {
        return leftmost_beam$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short leftmost_beam
     * }
     */
    public static short leftmost_beam(MemorySegment struct) {
        return struct.get(leftmost_beamLAYOUT, leftmost_beam$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short leftmost_beam
     * }
     */
    public static void leftmost_beam(MemorySegment struct, short fieldValue) {
        struct.set(leftmost_beamLAYOUT, leftmost_beam$OFFSET, fieldValue);
    }

    private static final OfShort rightmost_beamLAYOUT = (OfShort)LAYOUT.select(groupElement("rightmost_beam"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned short rightmost_beam
     * }
     */
    public static final OfShort rightmost_beamLAYOUT() {
        return rightmost_beamLAYOUT;
    }

    private static final long rightmost_beam$OFFSET = 2;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned short rightmost_beam
     * }
     */
    public static final long rightmost_beam$offset() {
        return rightmost_beam$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short rightmost_beam
     * }
     */
    public static short rightmost_beam(MemorySegment struct) {
        return struct.get(rightmost_beamLAYOUT, rightmost_beam$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short rightmost_beam
     * }
     */
    public static void rightmost_beam(MemorySegment struct, short fieldValue) {
        struct.set(rightmost_beamLAYOUT, rightmost_beam$OFFSET, fieldValue);
    }

    private static final OfShort total_beamsLAYOUT = (OfShort)LAYOUT.select(groupElement("total_beams"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned short total_beams
     * }
     */
    public static final OfShort total_beamsLAYOUT() {
        return total_beamsLAYOUT;
    }

    private static final long total_beams$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned short total_beams
     * }
     */
    public static final long total_beams$offset() {
        return total_beams$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short total_beams
     * }
     */
    public static short total_beams(MemorySegment struct) {
        return struct.get(total_beamsLAYOUT, total_beams$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short total_beams
     * }
     */
    public static void total_beams(MemorySegment struct, short fieldValue) {
        struct.set(total_beamsLAYOUT, total_beams$OFFSET, fieldValue);
    }

    private static final OfShort nav_modeLAYOUT = (OfShort)LAYOUT.select(groupElement("nav_mode"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned short nav_mode
     * }
     */
    public static final OfShort nav_modeLAYOUT() {
        return nav_modeLAYOUT;
    }

    private static final long nav_mode$OFFSET = 6;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned short nav_mode
     * }
     */
    public static final long nav_mode$offset() {
        return nav_mode$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short nav_mode
     * }
     */
    public static short nav_mode(MemorySegment struct) {
        return struct.get(nav_modeLAYOUT, nav_mode$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short nav_mode
     * }
     */
    public static void nav_mode(MemorySegment struct, short fieldValue) {
        struct.set(nav_modeLAYOUT, nav_mode$OFFSET, fieldValue);
    }

    private static final OfShort ping_numberLAYOUT = (OfShort)LAYOUT.select(groupElement("ping_number"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned short ping_number
     * }
     */
    public static final OfShort ping_numberLAYOUT() {
        return ping_numberLAYOUT;
    }

    private static final long ping_number$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned short ping_number
     * }
     */
    public static final long ping_number$offset() {
        return ping_number$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short ping_number
     * }
     */
    public static short ping_number(MemorySegment struct) {
        return struct.get(ping_numberLAYOUT, ping_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short ping_number
     * }
     */
    public static void ping_number(MemorySegment struct, short fieldValue) {
        struct.set(ping_numberLAYOUT, ping_number$OFFSET, fieldValue);
    }

    private static final OfShort mission_numberLAYOUT = (OfShort)LAYOUT.select(groupElement("mission_number"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned short mission_number
     * }
     */
    public static final OfShort mission_numberLAYOUT() {
        return mission_numberLAYOUT;
    }

    private static final long mission_number$OFFSET = 10;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned short mission_number
     * }
     */
    public static final long mission_number$offset() {
        return mission_number$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short mission_number
     * }
     */
    public static short mission_number(MemorySegment struct) {
        return struct.get(mission_numberLAYOUT, mission_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short mission_number
     * }
     */
    public static void mission_number(MemorySegment struct, short fieldValue) {
        struct.set(mission_numberLAYOUT, mission_number$OFFSET, fieldValue);
    }

}

