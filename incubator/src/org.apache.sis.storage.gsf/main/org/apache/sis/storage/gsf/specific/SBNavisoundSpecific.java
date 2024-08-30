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
public final class SBNavisoundSpecific extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_DOUBLE.withName("pulse_length"),
        MemoryLayout.sequenceLayout(8, GSF.C_CHAR).withName("spare")
    ).withName("t_gsfSBNavisoundSpecific");

    public SBNavisoundSpecific(MemorySegment struct) {
        super(struct);
    }

    public SBNavisoundSpecific(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfDouble pulse_lengthLAYOUT = (OfDouble)LAYOUT.select(groupElement("pulse_length"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double pulse_length
     * }
     */
    public static final OfDouble pulse_lengthLAYOUT() {
        return pulse_lengthLAYOUT;
    }

    private static final long pulse_length$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double pulse_length
     * }
     */
    public static final long pulse_length$offset() {
        return pulse_length$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double pulse_length
     * }
     */
    public static double pulse_length(MemorySegment struct) {
        return struct.get(pulse_lengthLAYOUT, pulse_length$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double pulse_length
     * }
     */
    public static void pulse_length(MemorySegment struct, double fieldValue) {
        struct.set(pulse_lengthLAYOUT, pulse_length$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * char spare[8]
     * }
     */
    public static final SequenceLayout spareLAYOUT() {
        return spareLAYOUT;
    }

    private static final long spare$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * char spare[8]
     * }
     */
    public static final long spare$offset() {
        return spare$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char spare[8]
     * }
     */
    public static MemorySegment spare(MemorySegment struct) {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char spare[8]
     * }
     */
    public static void spare(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static long[] spare$DIMS = { 8 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * char spare[8]
     * }
     */
    public static long[] spare$dimensions() {
        return spare$DIMS;
    }
    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * char spare[8]
     * }
     */
    public static byte spare(MemorySegment struct, long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * char spare[8]
     * }
     */
    public static void spare(MemorySegment struct, long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

}

