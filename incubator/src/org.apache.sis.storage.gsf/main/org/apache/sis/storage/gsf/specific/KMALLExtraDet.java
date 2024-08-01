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
public final class KMALLExtraDet extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("numExtraDetInClass"),
        GSF.C_INT.withName("alarmFlag"),
        MemoryLayout.sequenceLayout(32, GSF.C_CHAR).withName("spare")
    ).withName("t_gsfKMALLExtraDetClass");

    public KMALLExtraDet(MemorySegment struct) {
        super(struct);
    }

    public KMALLExtraDet(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt numExtraDetInClassLAYOUT = (OfInt)LAYOUT.select(groupElement("numExtraDetInClass"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int numExtraDetInClass
     * }
     */
    public static final OfInt numExtraDetInClassLAYOUT() {
        return numExtraDetInClassLAYOUT;
    }

    private static final long numExtraDetInClass$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int numExtraDetInClass
     * }
     */
    public static final long numExtraDetInClass$offset() {
        return numExtraDetInClass$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int numExtraDetInClass
     * }
     */
    public static int numExtraDetInClass(MemorySegment struct) {
        return struct.get(numExtraDetInClassLAYOUT, numExtraDetInClass$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int numExtraDetInClass
     * }
     */
    public static void numExtraDetInClass(MemorySegment struct, int fieldValue) {
        struct.set(numExtraDetInClassLAYOUT, numExtraDetInClass$OFFSET, fieldValue);
    }

    private static final OfInt alarmFlagLAYOUT = (OfInt)LAYOUT.select(groupElement("alarmFlag"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int alarmFlag
     * }
     */
    public static final OfInt alarmFlagLAYOUT() {
        return alarmFlagLAYOUT;
    }

    private static final long alarmFlag$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int alarmFlag
     * }
     */
    public static final long alarmFlag$offset() {
        return alarmFlag$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int alarmFlag
     * }
     */
    public static int alarmFlag(MemorySegment struct) {
        return struct.get(alarmFlagLAYOUT, alarmFlag$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int alarmFlag
     * }
     */
    public static void alarmFlag(MemorySegment struct, int fieldValue) {
        struct.set(alarmFlagLAYOUT, alarmFlag$OFFSET, fieldValue);
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

    private static final long spare$OFFSET = 8;

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

