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
public final class SBBDB extends StructClass {
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

    public SBBDB(MemorySegment struct) {
        super(struct);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt doc_noLAYOUT = (OfInt)LAYOUT.select(groupElement("doc_no"));

    private static final long doc_no$OFFSET = 0;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int doc_no
     * }
     */
    public int doc_no() {
        return struct.get(doc_noLAYOUT, doc_no$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int doc_no
     * }
     */
    public void doc_no(int fieldValue) {
        struct.set(doc_noLAYOUT, doc_no$OFFSET, fieldValue);
    }

    private static final OfByte evalLAYOUT = (OfByte)LAYOUT.select(groupElement("eval"));

    private static final long eval$OFFSET = 4;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char eval
     * }
     */
    public byte eval() {
        return struct.get(evalLAYOUT, eval$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char eval
     * }
     */
    public void eval(byte fieldValue) {
        struct.set(evalLAYOUT, eval$OFFSET, fieldValue);
    }

    private static final OfByte classificationLAYOUT = (OfByte)LAYOUT.select(groupElement("classification"));

    private static final long classification$OFFSET = 5;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char classification
     * }
     */
    public byte classification() {
        return struct.get(classificationLAYOUT, classification$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char classification
     * }
     */
    public void classification(byte fieldValue) {
        struct.set(classificationLAYOUT, classification$OFFSET, fieldValue);
    }

    private static final OfByte track_adj_flagLAYOUT = (OfByte)LAYOUT.select(groupElement("track_adj_flag"));

    private static final long track_adj_flag$OFFSET = 6;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char track_adj_flag
     * }
     */
    public byte track_adj_flag() {
        return struct.get(track_adj_flagLAYOUT, track_adj_flag$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char track_adj_flag
     * }
     */
    public void track_adj_flag(byte fieldValue) {
        struct.set(track_adj_flagLAYOUT, track_adj_flag$OFFSET, fieldValue);
    }

    private static final OfByte source_flagLAYOUT = (OfByte)LAYOUT.select(groupElement("source_flag"));

    private static final long source_flag$OFFSET = 7;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char source_flag
     * }
     */
    public byte source_flag() {
        return struct.get(source_flagLAYOUT, source_flag$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char source_flag
     * }
     */
    public void source_flag(byte fieldValue) {
        struct.set(source_flagLAYOUT, source_flag$OFFSET, fieldValue);
    }

    private static final OfByte pt_or_track_lnLAYOUT = (OfByte)LAYOUT.select(groupElement("pt_or_track_ln"));

    private static final long pt_or_track_ln$OFFSET = 8;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char pt_or_track_ln
     * }
     */
    public byte pt_or_track_ln() {
        return struct.get(pt_or_track_lnLAYOUT, pt_or_track_ln$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char pt_or_track_ln
     * }
     */
    public void pt_or_track_ln(byte fieldValue) {
        struct.set(pt_or_track_lnLAYOUT, pt_or_track_ln$OFFSET, fieldValue);
    }

    private static final OfByte datum_flagLAYOUT = (OfByte)LAYOUT.select(groupElement("datum_flag"));

    private static final long datum_flag$OFFSET = 9;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char datum_flag
     * }
     */
    public byte datum_flag() {
        return struct.get(datum_flagLAYOUT, datum_flag$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char datum_flag
     * }
     */
    public void datum_flag(byte fieldValue) {
        struct.set(datum_flagLAYOUT, datum_flag$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    private static final long spare$OFFSET = 10;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public MemorySegment spare() {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public void spare(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public byte spare(long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * char spare[4]
     * }
     */
    public void spare(long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }
}
