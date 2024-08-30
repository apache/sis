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
public final class EM12Specific extends StructClass {


    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("ping_number"),
        GSF.C_INT.withName("resolution"),
        GSF.C_INT.withName("ping_quality"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("sound_velocity"),
        GSF.C_INT.withName("mode"),
        MemoryLayout.sequenceLayout(32, GSF.C_CHAR).withName("spare"),
        MemoryLayout.paddingLayout(4)
    ).withName("t_gsfEM12Specific");

    public EM12Specific(MemorySegment struct) {
        super(struct);
    }

    public EM12Specific(SegmentAllocator allocator) {
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

    private static final OfInt resolutionLAYOUT = (OfInt)LAYOUT.select(groupElement("resolution"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int resolution
     * }
     */
    public static final OfInt resolutionLAYOUT() {
        return resolutionLAYOUT;
    }

    private static final long resolution$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int resolution
     * }
     */
    public static final long resolution$offset() {
        return resolution$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int resolution
     * }
     */
    public static int resolution(MemorySegment struct) {
        return struct.get(resolutionLAYOUT, resolution$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int resolution
     * }
     */
    public static void resolution(MemorySegment struct, int fieldValue) {
        struct.set(resolutionLAYOUT, resolution$OFFSET, fieldValue);
    }

    private static final OfInt ping_qualityLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_quality"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int ping_quality
     * }
     */
    public static final OfInt ping_qualityLAYOUT() {
        return ping_qualityLAYOUT;
    }

    private static final long ping_quality$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int ping_quality
     * }
     */
    public static final long ping_quality$offset() {
        return ping_quality$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_quality
     * }
     */
    public static int ping_quality(MemorySegment struct) {
        return struct.get(ping_qualityLAYOUT, ping_quality$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_quality
     * }
     */
    public static void ping_quality(MemorySegment struct, int fieldValue) {
        struct.set(ping_qualityLAYOUT, ping_quality$OFFSET, fieldValue);
    }

    private static final OfDouble sound_velocityLAYOUT = (OfDouble)LAYOUT.select(groupElement("sound_velocity"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double sound_velocity
     * }
     */
    public static final OfDouble sound_velocityLAYOUT() {
        return sound_velocityLAYOUT;
    }

    private static final long sound_velocity$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double sound_velocity
     * }
     */
    public static final long sound_velocity$offset() {
        return sound_velocity$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double sound_velocity
     * }
     */
    public static double sound_velocity(MemorySegment struct) {
        return struct.get(sound_velocityLAYOUT, sound_velocity$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double sound_velocity
     * }
     */
    public static void sound_velocity(MemorySegment struct, double fieldValue) {
        struct.set(sound_velocityLAYOUT, sound_velocity$OFFSET, fieldValue);
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

    private static final long mode$OFFSET = 24;

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

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char spare[32]
     * }
     */
    public static final SequenceLayout spareLAYOUT() {
        return spareLAYOUT;
    }

    private static final long spare$OFFSET = 28;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char spare[32]
     * }
     */
    public static final long spare$offset() {
        return spare$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare[32]
     * }
     */
    public static MemorySegment spare(MemorySegment struct) {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char spare[32]
     * }
     */
    public static void spare(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static long[] spare$DIMS = { 32 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * unsigned char spare[32]
     * }
     */
    public static long[] spare$dimensions() {
        return spare$DIMS;
    }
    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char spare[32]
     * }
     */
    public static byte spare(MemorySegment struct, long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char spare[32]
     * }
     */
    public static void spare(MemorySegment struct, long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

}

