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
public final class SBEchotracSpecific extends StructClass {
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("navigation_error"),
        GSF.C_SHORT.withName("mpp_source"),
        GSF.C_SHORT.withName("tide_source"),
        GSF.C_DOUBLE.withName("dynamic_draft"),
        MemoryLayout.sequenceLayout(4, GSF.C_CHAR).withName("spare"),
        MemoryLayout.paddingLayout(4)
    ).withName("t_gsfSBEchotracSpecific");

    public SBEchotracSpecific(MemorySegment struct) {
        super(struct);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt navigation_errorLAYOUT = (OfInt)LAYOUT.select(groupElement("navigation_error"));

    private static final long navigation_error$OFFSET = 0;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int navigation_error
     * }
     */
    public int navigation_error() {
        return struct.get(navigation_errorLAYOUT, navigation_error$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int navigation_error
     * }
     */
    public void navigation_error(int fieldValue) {
        struct.set(navigation_errorLAYOUT, navigation_error$OFFSET, fieldValue);
    }

    private static final OfShort mpp_sourceLAYOUT = (OfShort)LAYOUT.select(groupElement("mpp_source"));

    private static final long mpp_source$OFFSET = 4;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short mpp_source
     * }
     */
    public short mpp_source() {
        return struct.get(mpp_sourceLAYOUT, mpp_source$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short mpp_source
     * }
     */
    public void mpp_source(short fieldValue) {
        struct.set(mpp_sourceLAYOUT, mpp_source$OFFSET, fieldValue);
    }

    private static final OfShort tide_sourceLAYOUT = (OfShort)LAYOUT.select(groupElement("tide_source"));

    private static final long tide_source$OFFSET = 6;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short tide_source
     * }
     */
    public short tide_source() {
        return struct.get(tide_sourceLAYOUT, tide_source$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short tide_source
     * }
     */
    public void tide_source(short fieldValue) {
        struct.set(tide_sourceLAYOUT, tide_source$OFFSET, fieldValue);
    }

    private static final OfDouble dynamic_draftLAYOUT = (OfDouble)LAYOUT.select(groupElement("dynamic_draft"));

    private static final long dynamic_draft$OFFSET = 8;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double dynamic_draft
     * }
     */
    public double dynamic_draft() {
        return struct.get(dynamic_draftLAYOUT, dynamic_draft$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double dynamic_draft
     * }
     */
    public void dynamic_draft(double fieldValue) {
        struct.set(dynamic_draftLAYOUT, dynamic_draft$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    private static final long spare$OFFSET = 16;

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
