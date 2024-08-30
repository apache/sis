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
public final class EM3ImagerySpecific extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_SHORT.withName("range_norm"),
        GSF.C_SHORT.withName("start_tvg_ramp"),
        GSF.C_SHORT.withName("stop_tvg_ramp"),
        GSF.C_CHAR.withName("bsn"),
        GSF.C_CHAR.withName("bso"),
        GSF.C_DOUBLE.withName("mean_absorption"),
        GSF.C_SHORT.withName("offset"),
        GSF.C_SHORT.withName("scale"),
        MemoryLayout.sequenceLayout(4, GSF.C_CHAR).withName("spare")
    ).withName("t_gsfEM3ImagerySpecific");

    public EM3ImagerySpecific(MemorySegment struct) {
        super(struct);
    }

    public EM3ImagerySpecific(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfShort range_normLAYOUT = (OfShort)LAYOUT.select(groupElement("range_norm"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned short range_norm
     * }
     */
    public static final OfShort range_normLAYOUT() {
        return range_normLAYOUT;
    }

    private static final long range_norm$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned short range_norm
     * }
     */
    public static final long range_norm$offset() {
        return range_norm$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short range_norm
     * }
     */
    public static short range_norm(MemorySegment struct) {
        return struct.get(range_normLAYOUT, range_norm$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short range_norm
     * }
     */
    public static void range_norm(MemorySegment struct, short fieldValue) {
        struct.set(range_normLAYOUT, range_norm$OFFSET, fieldValue);
    }

    private static final OfShort start_tvg_rampLAYOUT = (OfShort)LAYOUT.select(groupElement("start_tvg_ramp"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned short start_tvg_ramp
     * }
     */
    public static final OfShort start_tvg_rampLAYOUT() {
        return start_tvg_rampLAYOUT;
    }

    private static final long start_tvg_ramp$OFFSET = 2;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned short start_tvg_ramp
     * }
     */
    public static final long start_tvg_ramp$offset() {
        return start_tvg_ramp$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short start_tvg_ramp
     * }
     */
    public static short start_tvg_ramp(MemorySegment struct) {
        return struct.get(start_tvg_rampLAYOUT, start_tvg_ramp$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short start_tvg_ramp
     * }
     */
    public static void start_tvg_ramp(MemorySegment struct, short fieldValue) {
        struct.set(start_tvg_rampLAYOUT, start_tvg_ramp$OFFSET, fieldValue);
    }

    private static final OfShort stop_tvg_rampLAYOUT = (OfShort)LAYOUT.select(groupElement("stop_tvg_ramp"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned short stop_tvg_ramp
     * }
     */
    public static final OfShort stop_tvg_rampLAYOUT() {
        return stop_tvg_rampLAYOUT;
    }

    private static final long stop_tvg_ramp$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned short stop_tvg_ramp
     * }
     */
    public static final long stop_tvg_ramp$offset() {
        return stop_tvg_ramp$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short stop_tvg_ramp
     * }
     */
    public static short stop_tvg_ramp(MemorySegment struct) {
        return struct.get(stop_tvg_rampLAYOUT, stop_tvg_ramp$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short stop_tvg_ramp
     * }
     */
    public static void stop_tvg_ramp(MemorySegment struct, short fieldValue) {
        struct.set(stop_tvg_rampLAYOUT, stop_tvg_ramp$OFFSET, fieldValue);
    }

    private static final OfByte bsnLAYOUT = (OfByte)LAYOUT.select(groupElement("bsn"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * char bsn
     * }
     */
    public static final OfByte bsnLAYOUT() {
        return bsnLAYOUT;
    }

    private static final long bsn$OFFSET = 6;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * char bsn
     * }
     */
    public static final long bsn$offset() {
        return bsn$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char bsn
     * }
     */
    public static byte bsn(MemorySegment struct) {
        return struct.get(bsnLAYOUT, bsn$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char bsn
     * }
     */
    public static void bsn(MemorySegment struct, byte fieldValue) {
        struct.set(bsnLAYOUT, bsn$OFFSET, fieldValue);
    }

    private static final OfByte bsoLAYOUT = (OfByte)LAYOUT.select(groupElement("bso"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * char bso
     * }
     */
    public static final OfByte bsoLAYOUT() {
        return bsoLAYOUT;
    }

    private static final long bso$OFFSET = 7;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * char bso
     * }
     */
    public static final long bso$offset() {
        return bso$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char bso
     * }
     */
    public static byte bso(MemorySegment struct) {
        return struct.get(bsoLAYOUT, bso$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char bso
     * }
     */
    public static void bso(MemorySegment struct, byte fieldValue) {
        struct.set(bsoLAYOUT, bso$OFFSET, fieldValue);
    }

    private static final OfDouble mean_absorptionLAYOUT = (OfDouble)LAYOUT.select(groupElement("mean_absorption"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double mean_absorption
     * }
     */
    public static final OfDouble mean_absorptionLAYOUT() {
        return mean_absorptionLAYOUT;
    }

    private static final long mean_absorption$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double mean_absorption
     * }
     */
    public static final long mean_absorption$offset() {
        return mean_absorption$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double mean_absorption
     * }
     */
    public static double mean_absorption(MemorySegment struct) {
        return struct.get(mean_absorptionLAYOUT, mean_absorption$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double mean_absorption
     * }
     */
    public static void mean_absorption(MemorySegment struct, double fieldValue) {
        struct.set(mean_absorptionLAYOUT, mean_absorption$OFFSET, fieldValue);
    }

    private static final OfShort offsetLAYOUT = (OfShort)LAYOUT.select(groupElement("offset"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * short offset
     * }
     */
    public static final OfShort offsetLAYOUT() {
        return offsetLAYOUT;
    }

    private static final long offset$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * short offset
     * }
     */
    public static final long offset$offset() {
        return offset$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * short offset
     * }
     */
    public static short offset(MemorySegment struct) {
        return struct.get(offsetLAYOUT, offset$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * short offset
     * }
     */
    public static void offset(MemorySegment struct, short fieldValue) {
        struct.set(offsetLAYOUT, offset$OFFSET, fieldValue);
    }

    private static final OfShort scaleLAYOUT = (OfShort)LAYOUT.select(groupElement("scale"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * short scale
     * }
     */
    public static final OfShort scaleLAYOUT() {
        return scaleLAYOUT;
    }

    private static final long scale$OFFSET = 18;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * short scale
     * }
     */
    public static final long scale$offset() {
        return scale$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * short scale
     * }
     */
    public static short scale(MemorySegment struct) {
        return struct.get(scaleLAYOUT, scale$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * short scale
     * }
     */
    public static void scale(MemorySegment struct, short fieldValue) {
        struct.set(scaleLAYOUT, scale$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned char spare[4]
     * }
     */
    public static final SequenceLayout spareLAYOUT() {
        return spareLAYOUT;
    }

    private static final long spare$OFFSET = 20;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned char spare[4]
     * }
     */
    public static final long spare$offset() {
        return spare$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare[4]
     * }
     */
    public static MemorySegment spare(MemorySegment struct) {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char spare[4]
     * }
     */
    public static void spare(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static long[] spare$DIMS = { 4 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * unsigned char spare[4]
     * }
     */
    public static long[] spare$dimensions() {
        return spare$DIMS;
    }
    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char spare[4]
     * }
     */
    public static byte spare(MemorySegment struct, long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char spare[4]
     * }
     */
    public static void spare(MemorySegment struct, long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

}

