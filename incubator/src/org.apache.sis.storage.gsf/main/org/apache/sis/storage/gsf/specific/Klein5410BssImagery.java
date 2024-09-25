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
public final class Klein5410BssImagery extends StructClass {
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("res_mode"),
        GSF.C_INT.withName("tvg_page"),
        MemoryLayout.sequenceLayout(5, GSF.C_INT).withName("beam_id"),
        MemoryLayout.sequenceLayout(4, GSF.C_CHAR).withName("spare")
    ).withName("t_gsfKlein5410BssImagerySpecific");

    public Klein5410BssImagery(MemorySegment struct) {
        super(struct);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt res_modeLAYOUT = (OfInt)LAYOUT.select(groupElement("res_mode"));

    private static final long res_mode$OFFSET = 0;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int res_mode
     * }
     */
    public int res_mode() {
        return struct.get(res_modeLAYOUT, res_mode$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int res_mode
     * }
     */
    public void res_mode(int fieldValue) {
        struct.set(res_modeLAYOUT, res_mode$OFFSET, fieldValue);
    }

    private static final OfInt tvg_pageLAYOUT = (OfInt)LAYOUT.select(groupElement("tvg_page"));

    private static final long tvg_page$OFFSET = 4;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int tvg_page
     * }
     */
    public int tvg_page() {
        return struct.get(tvg_pageLAYOUT, tvg_page$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int tvg_page
     * }
     */
    public void tvg_page(int fieldValue) {
        struct.set(tvg_pageLAYOUT, tvg_page$OFFSET, fieldValue);
    }

    private static final SequenceLayout beam_idLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("beam_id"));

    private static final long beam_id$OFFSET = 8;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int beam_id[5]
     * }
     */
    public MemorySegment beam_id() {
        return struct.asSlice(beam_id$OFFSET, beam_idLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int beam_id[5]
     * }
     */
    public void beam_id(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, beam_id$OFFSET, beam_idLAYOUT.byteSize());
    }

    private static final VarHandle beam_id$ELEM_HANDLE = beam_idLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned int beam_id[5]
     * }
     */
    public int beam_id(long index0) {
        return (int)beam_id$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned int beam_id[5]
     * }
     */
    public void beam_id(long index0, int fieldValue) {
        beam_id$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    private static final long spare$OFFSET = 28;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare[4]
     * }
     */
    public MemorySegment spare() {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char spare[4]
     * }
     */
    public void spare(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char spare[4]
     * }
     */
    public byte spare(long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char spare[4]
     * }
     */
    public void spare(long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }
}
