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
public final class SBBDBSpecific extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("doc_no"),
        GSF.C_CHAR.withName("eval"),
        GSF.C_CHAR.withName("classification"),
        GSF.C_CHAR.withName("track_adj_flag"),
        GSF.C_CHAR.withName("source_flag"),
        GSF.C_CHAR.withName("pt_or_track_ln"),
        GSF.C_CHAR.withName("datum_flag"),
        MemoryLayout.sequenceLayout(4, GSF.C_CHAR).withName("spare"),
        MemoryLayout.paddingLayout(2)
    ).withName("t_gsfSBBDBSpecific");

    public SBBDBSpecific(MemorySegment struct) {
        super(struct);
    }

    public SBBDBSpecific(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt doc_noLAYOUT = (OfInt)LAYOUT.select(groupElement("doc_no"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int doc_no
     * }
     */
    public static final OfInt doc_noLAYOUT() {
        return doc_noLAYOUT;
    }

    private static final long doc_no$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int doc_no
     * }
     */
    public static final long doc_no$offset() {
        return doc_no$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int doc_no
     * }
     */
    public static int doc_no(MemorySegment struct) {
        return struct.get(doc_noLAYOUT, doc_no$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int doc_no
     * }
     */
    public static void doc_no(MemorySegment struct, int fieldValue) {
        struct.set(doc_noLAYOUT, doc_no$OFFSET, fieldValue);
    }

    private static final OfByte evalLAYOUT = (OfByte)LAYOUT.select(groupElement("eval"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * char eval
     * }
     */
    public static final OfByte evalLAYOUT() {
        return evalLAYOUT;
    }

    private static final long eval$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * char eval
     * }
     */
    public static final long eval$offset() {
        return eval$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char eval
     * }
     */
    public static byte eval(MemorySegment struct) {
        return struct.get(evalLAYOUT, eval$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char eval
     * }
     */
    public static void eval(MemorySegment struct, byte fieldValue) {
        struct.set(evalLAYOUT, eval$OFFSET, fieldValue);
    }

    private static final OfByte classificationLAYOUT = (OfByte)LAYOUT.select(groupElement("classification"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * char classification
     * }
     */
    public static final OfByte classificationLAYOUT() {
        return classificationLAYOUT;
    }

    private static final long classification$OFFSET = 5;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * char classification
     * }
     */
    public static final long classification$offset() {
        return classification$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char classification
     * }
     */
    public static byte classification(MemorySegment struct) {
        return struct.get(classificationLAYOUT, classification$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char classification
     * }
     */
    public static void classification(MemorySegment struct, byte fieldValue) {
        struct.set(classificationLAYOUT, classification$OFFSET, fieldValue);
    }

    private static final OfByte track_adj_flagLAYOUT = (OfByte)LAYOUT.select(groupElement("track_adj_flag"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * char track_adj_flag
     * }
     */
    public static final OfByte track_adj_flagLAYOUT() {
        return track_adj_flagLAYOUT;
    }

    private static final long track_adj_flag$OFFSET = 6;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * char track_adj_flag
     * }
     */
    public static final long track_adj_flag$offset() {
        return track_adj_flag$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char track_adj_flag
     * }
     */
    public static byte track_adj_flag(MemorySegment struct) {
        return struct.get(track_adj_flagLAYOUT, track_adj_flag$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char track_adj_flag
     * }
     */
    public static void track_adj_flag(MemorySegment struct, byte fieldValue) {
        struct.set(track_adj_flagLAYOUT, track_adj_flag$OFFSET, fieldValue);
    }

    private static final OfByte source_flagLAYOUT = (OfByte)LAYOUT.select(groupElement("source_flag"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * char source_flag
     * }
     */
    public static final OfByte source_flagLAYOUT() {
        return source_flagLAYOUT;
    }

    private static final long source_flag$OFFSET = 7;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * char source_flag
     * }
     */
    public static final long source_flag$offset() {
        return source_flag$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char source_flag
     * }
     */
    public static byte source_flag(MemorySegment struct) {
        return struct.get(source_flagLAYOUT, source_flag$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char source_flag
     * }
     */
    public static void source_flag(MemorySegment struct, byte fieldValue) {
        struct.set(source_flagLAYOUT, source_flag$OFFSET, fieldValue);
    }

    private static final OfByte pt_or_track_lnLAYOUT = (OfByte)LAYOUT.select(groupElement("pt_or_track_ln"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * char pt_or_track_ln
     * }
     */
    public static final OfByte pt_or_track_lnLAYOUT() {
        return pt_or_track_lnLAYOUT;
    }

    private static final long pt_or_track_ln$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * char pt_or_track_ln
     * }
     */
    public static final long pt_or_track_ln$offset() {
        return pt_or_track_ln$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char pt_or_track_ln
     * }
     */
    public static byte pt_or_track_ln(MemorySegment struct) {
        return struct.get(pt_or_track_lnLAYOUT, pt_or_track_ln$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char pt_or_track_ln
     * }
     */
    public static void pt_or_track_ln(MemorySegment struct, byte fieldValue) {
        struct.set(pt_or_track_lnLAYOUT, pt_or_track_ln$OFFSET, fieldValue);
    }

    private static final OfByte datum_flagLAYOUT = (OfByte)LAYOUT.select(groupElement("datum_flag"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * char datum_flag
     * }
     */
    public static final OfByte datum_flagLAYOUT() {
        return datum_flagLAYOUT;
    }

    private static final long datum_flag$OFFSET = 9;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * char datum_flag
     * }
     */
    public static final long datum_flag$offset() {
        return datum_flag$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char datum_flag
     * }
     */
    public static byte datum_flag(MemorySegment struct) {
        return struct.get(datum_flagLAYOUT, datum_flag$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char datum_flag
     * }
     */
    public static void datum_flag(MemorySegment struct, byte fieldValue) {
        struct.set(datum_flagLAYOUT, datum_flag$OFFSET, fieldValue);
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

    private static final long spare$OFFSET = 10;

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

