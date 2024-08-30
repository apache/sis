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
public final class SBAmpSpecific extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_CHAR.withName("hour"),
        GSF.C_CHAR.withName("minute"),
        GSF.C_CHAR.withName("second"),
        GSF.C_CHAR.withName("hundredths"),
        GSF.C_INT.withName("block_number"),
        GSF.C_SHORT.withName("avg_gate_depth"),
        MemoryLayout.paddingLayout(2)
    ).withName("t_gsfSBAmpSpecific");

    public SBAmpSpecific(MemorySegment struct) {
        super(struct);
    }

    public SBAmpSpecific(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfByte hourLAYOUT = (OfByte)LAYOUT.select(groupElement("hour"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char hour
     * }
     */
    public static final OfByte hourLAYOUT() {
        return hourLAYOUT;
    }

    private static final long hour$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char hour
     * }
     */
    public static final long hour$offset() {
        return hour$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char hour
     * }
     */
    public static byte hour(MemorySegment struct) {
        return struct.get(hourLAYOUT, hour$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char hour
     * }
     */
    public static void hour(MemorySegment struct, byte fieldValue) {
        struct.set(hourLAYOUT, hour$OFFSET, fieldValue);
    }

    private static final OfByte minuteLAYOUT = (OfByte)LAYOUT.select(groupElement("minute"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char minute
     * }
     */
    public static final OfByte minuteLAYOUT() {
        return minuteLAYOUT;
    }

    private static final long minute$OFFSET = 1;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char minute
     * }
     */
    public static final long minute$offset() {
        return minute$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char minute
     * }
     */
    public static byte minute(MemorySegment struct) {
        return struct.get(minuteLAYOUT, minute$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char minute
     * }
     */
    public static void minute(MemorySegment struct, byte fieldValue) {
        struct.set(minuteLAYOUT, minute$OFFSET, fieldValue);
    }

    private static final OfByte secondLAYOUT = (OfByte)LAYOUT.select(groupElement("second"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char second
     * }
     */
    public static final OfByte secondLAYOUT() {
        return secondLAYOUT;
    }

    private static final long second$OFFSET = 2;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char second
     * }
     */
    public static final long second$offset() {
        return second$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char second
     * }
     */
    public static byte second(MemorySegment struct) {
        return struct.get(secondLAYOUT, second$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char second
     * }
     */
    public static void second(MemorySegment struct, byte fieldValue) {
        struct.set(secondLAYOUT, second$OFFSET, fieldValue);
    }

    private static final OfByte hundredthsLAYOUT = (OfByte)LAYOUT.select(groupElement("hundredths"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char hundredths
     * }
     */
    public static final OfByte hundredthsLAYOUT() {
        return hundredthsLAYOUT;
    }

    private static final long hundredths$OFFSET = 3;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char hundredths
     * }
     */
    public static final long hundredths$offset() {
        return hundredths$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char hundredths
     * }
     */
    public static byte hundredths(MemorySegment struct) {
        return struct.get(hundredthsLAYOUT, hundredths$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char hundredths
     * }
     */
    public static void hundredths(MemorySegment struct, byte fieldValue) {
        struct.set(hundredthsLAYOUT, hundredths$OFFSET, fieldValue);
    }

    private static final OfInt block_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("block_number"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned int block_number
     * }
     */
    public static final OfInt block_numberLAYOUT() {
        return block_numberLAYOUT;
    }

    private static final long block_number$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned int block_number
     * }
     */
    public static final long block_number$offset() {
        return block_number$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int block_number
     * }
     */
    public static int block_number(MemorySegment struct) {
        return struct.get(block_numberLAYOUT, block_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int block_number
     * }
     */
    public static void block_number(MemorySegment struct, int fieldValue) {
        struct.set(block_numberLAYOUT, block_number$OFFSET, fieldValue);
    }

    private static final OfShort avg_gate_depthLAYOUT = (OfShort)LAYOUT.select(groupElement("avg_gate_depth"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * short avg_gate_depth
     * }
     */
    public static final OfShort avg_gate_depthLAYOUT() {
        return avg_gate_depthLAYOUT;
    }

    private static final long avg_gate_depth$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * short avg_gate_depth
     * }
     */
    public static final long avg_gate_depth$offset() {
        return avg_gate_depth$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * short avg_gate_depth
     * }
     */
    public static short avg_gate_depth(MemorySegment struct) {
        return struct.get(avg_gate_depthLAYOUT, avg_gate_depth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * short avg_gate_depth
     * }
     */
    public static void avg_gate_depth(MemorySegment struct, short fieldValue) {
        struct.set(avg_gate_depthLAYOUT, avg_gate_depth$OFFSET, fieldValue);
    }

}

