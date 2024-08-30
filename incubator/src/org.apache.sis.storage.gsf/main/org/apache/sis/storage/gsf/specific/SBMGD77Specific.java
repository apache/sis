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
public final class SBMGD77Specific extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_SHORT.withName("time_zone_corr"),
        GSF.C_SHORT.withName("position_type_code"),
        GSF.C_SHORT.withName("correction_code"),
        GSF.C_SHORT.withName("bathy_type_code"),
        GSF.C_SHORT.withName("quality_code"),
        MemoryLayout.paddingLayout(6),
        GSF.C_DOUBLE.withName("travel_time"),
        MemoryLayout.sequenceLayout(4, GSF.C_CHAR).withName("spare"),
        MemoryLayout.paddingLayout(4)
    ).withName("t_gsfSBMGD77Specific");

    public SBMGD77Specific(MemorySegment struct) {
        super(struct);
    }

    public SBMGD77Specific(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfShort time_zone_corrLAYOUT = (OfShort)LAYOUT.select(groupElement("time_zone_corr"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned short time_zone_corr
     * }
     */
    public static final OfShort time_zone_corrLAYOUT() {
        return time_zone_corrLAYOUT;
    }

    private static final long time_zone_corr$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned short time_zone_corr
     * }
     */
    public static final long time_zone_corr$offset() {
        return time_zone_corr$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short time_zone_corr
     * }
     */
    public static short time_zone_corr(MemorySegment struct) {
        return struct.get(time_zone_corrLAYOUT, time_zone_corr$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short time_zone_corr
     * }
     */
    public static void time_zone_corr(MemorySegment struct, short fieldValue) {
        struct.set(time_zone_corrLAYOUT, time_zone_corr$OFFSET, fieldValue);
    }

    private static final OfShort position_type_codeLAYOUT = (OfShort)LAYOUT.select(groupElement("position_type_code"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned short position_type_code
     * }
     */
    public static final OfShort position_type_codeLAYOUT() {
        return position_type_codeLAYOUT;
    }

    private static final long position_type_code$OFFSET = 2;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned short position_type_code
     * }
     */
    public static final long position_type_code$offset() {
        return position_type_code$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short position_type_code
     * }
     */
    public static short position_type_code(MemorySegment struct) {
        return struct.get(position_type_codeLAYOUT, position_type_code$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short position_type_code
     * }
     */
    public static void position_type_code(MemorySegment struct, short fieldValue) {
        struct.set(position_type_codeLAYOUT, position_type_code$OFFSET, fieldValue);
    }

    private static final OfShort correction_codeLAYOUT = (OfShort)LAYOUT.select(groupElement("correction_code"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned short correction_code
     * }
     */
    public static final OfShort correction_codeLAYOUT() {
        return correction_codeLAYOUT;
    }

    private static final long correction_code$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned short correction_code
     * }
     */
    public static final long correction_code$offset() {
        return correction_code$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short correction_code
     * }
     */
    public static short correction_code(MemorySegment struct) {
        return struct.get(correction_codeLAYOUT, correction_code$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short correction_code
     * }
     */
    public static void correction_code(MemorySegment struct, short fieldValue) {
        struct.set(correction_codeLAYOUT, correction_code$OFFSET, fieldValue);
    }

    private static final OfShort bathy_type_codeLAYOUT = (OfShort)LAYOUT.select(groupElement("bathy_type_code"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned short bathy_type_code
     * }
     */
    public static final OfShort bathy_type_codeLAYOUT() {
        return bathy_type_codeLAYOUT;
    }

    private static final long bathy_type_code$OFFSET = 6;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned short bathy_type_code
     * }
     */
    public static final long bathy_type_code$offset() {
        return bathy_type_code$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short bathy_type_code
     * }
     */
    public static short bathy_type_code(MemorySegment struct) {
        return struct.get(bathy_type_codeLAYOUT, bathy_type_code$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short bathy_type_code
     * }
     */
    public static void bathy_type_code(MemorySegment struct, short fieldValue) {
        struct.set(bathy_type_codeLAYOUT, bathy_type_code$OFFSET, fieldValue);
    }

    private static final OfShort quality_codeLAYOUT = (OfShort)LAYOUT.select(groupElement("quality_code"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned short quality_code
     * }
     */
    public static final OfShort quality_codeLAYOUT() {
        return quality_codeLAYOUT;
    }

    private static final long quality_code$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned short quality_code
     * }
     */
    public static final long quality_code$offset() {
        return quality_code$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short quality_code
     * }
     */
    public static short quality_code(MemorySegment struct) {
        return struct.get(quality_codeLAYOUT, quality_code$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short quality_code
     * }
     */
    public static void quality_code(MemorySegment struct, short fieldValue) {
        struct.set(quality_codeLAYOUT, quality_code$OFFSET, fieldValue);
    }

    private static final OfDouble travel_timeLAYOUT = (OfDouble)LAYOUT.select(groupElement("travel_time"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double travel_time
     * }
     */
    public static final OfDouble travel_timeLAYOUT() {
        return travel_timeLAYOUT;
    }

    private static final long travel_time$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double travel_time
     * }
     */
    public static final long travel_time$offset() {
        return travel_time$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double travel_time
     * }
     */
    public static double travel_time(MemorySegment struct) {
        return struct.get(travel_timeLAYOUT, travel_time$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double travel_time
     * }
     */
    public static void travel_time(MemorySegment struct, double fieldValue) {
        struct.set(travel_timeLAYOUT, travel_time$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public static final SequenceLayout spareLAYOUT() {
        return spareLAYOUT;
    }

    private static final long spare$OFFSET = 24;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public static final long spare$offset() {
        return spare$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public static MemorySegment spare(MemorySegment struct) {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public static void spare(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static long[] spare$DIMS = { 4 };

    /**
     * Dimensions for array field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public static long[] spare$dimensions() {
        return spare$DIMS;
    }
    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public static byte spare(MemorySegment struct, long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public static void spare(MemorySegment struct, long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

}

