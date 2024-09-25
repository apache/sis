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
import org.apache.sis.storage.gsf.GSF;
import org.apache.sis.storage.gsf.StructClass;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class BDBSpecific extends StructClass {
    private static final OfInt LAYOUT_DOC_NO;
    private static final OfByte LAYOUT_EVAL;
    private static final OfByte LAYOUT_CLASSIFICATION;
    private static final OfByte LAYOUT_TRACK_ADJ_FLAG;
    private static final OfByte LAYOUT_SOURCE_FLAG;
    private static final OfByte LAYOUT_PT_OR_TRACK_LN;
    private static final OfByte LAYOUT_DATUM_FLAG;
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        LAYOUT_DOC_NO = GSF.C_INT.withName("doc_no"),
        LAYOUT_EVAL = GSF.C_CHAR.withName("eval"),
        LAYOUT_CLASSIFICATION = GSF.C_CHAR.withName("classification"),
        LAYOUT_TRACK_ADJ_FLAG = GSF.C_CHAR.withName("track_adj_flag"),
        LAYOUT_SOURCE_FLAG = GSF.C_CHAR.withName("source_flag"),
        LAYOUT_PT_OR_TRACK_LN = GSF.C_CHAR.withName("pt_or_track_ln"),
        LAYOUT_DATUM_FLAG = GSF.C_CHAR.withName("datum_flag"),
        MemoryLayout.paddingLayout(2)
    ).withName("t_gsfBDBSpecific");

    public BDBSpecific(MemorySegment struct) {
        super(struct);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int doc_no
     * }
     */
    public int getDocNo() {
        return struct.get(LAYOUT_DOC_NO, 0);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char eval
     * }
     */
    public byte getEval() {
        return struct.get(LAYOUT_EVAL, 4);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char classification
     * }
     */
    public byte getClassification() {
        return struct.get(LAYOUT_CLASSIFICATION, 5);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char track_adj_flag
     * }
     */
    public byte getTrackAdjFlag() {
        return struct.get(LAYOUT_TRACK_ADJ_FLAG, 6);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char source_flag
     * }
     */
    public byte getSourceFlag() {
        return struct.get(LAYOUT_SOURCE_FLAG, 7);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char pt_or_track_ln
     * }
     */
    public byte getPtOrTrackln() {
        return struct.get(LAYOUT_PT_OR_TRACK_LN, 8);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char datum_flag
     * }
     */
    public byte getDatumFlag() {
        return struct.get(LAYOUT_DATUM_FLAG, 9);
    }
}
