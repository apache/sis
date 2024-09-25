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

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfShort range_normLAYOUT = (OfShort)LAYOUT.select(groupElement("range_norm"));

    private static final long range_norm$OFFSET = 0;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short range_norm
     * }
     */
    public short range_norm() {
        return struct.get(range_normLAYOUT, range_norm$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short range_norm
     * }
     */
    public void range_norm(short fieldValue) {
        struct.set(range_normLAYOUT, range_norm$OFFSET, fieldValue);
    }

    private static final OfShort start_tvg_rampLAYOUT = (OfShort)LAYOUT.select(groupElement("start_tvg_ramp"));

    private static final long start_tvg_ramp$OFFSET = 2;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short start_tvg_ramp
     * }
     */
    public short start_tvg_ramp() {
        return struct.get(start_tvg_rampLAYOUT, start_tvg_ramp$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short start_tvg_ramp
     * }
     */
    public void start_tvg_ramp(short fieldValue) {
        struct.set(start_tvg_rampLAYOUT, start_tvg_ramp$OFFSET, fieldValue);
    }

    private static final OfShort stop_tvg_rampLAYOUT = (OfShort)LAYOUT.select(groupElement("stop_tvg_ramp"));

    private static final long stop_tvg_ramp$OFFSET = 4;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short stop_tvg_ramp
     * }
     */
    public short stop_tvg_ramp() {
        return struct.get(stop_tvg_rampLAYOUT, stop_tvg_ramp$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short stop_tvg_ramp
     * }
     */
    public void stop_tvg_ramp(short fieldValue) {
        struct.set(stop_tvg_rampLAYOUT, stop_tvg_ramp$OFFSET, fieldValue);
    }

    private static final OfByte bsnLAYOUT = (OfByte)LAYOUT.select(groupElement("bsn"));

    private static final long bsn$OFFSET = 6;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char bsn
     * }
     */
    public byte bsn() {
        return struct.get(bsnLAYOUT, bsn$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char bsn
     * }
     */
    public void bsn(byte fieldValue) {
        struct.set(bsnLAYOUT, bsn$OFFSET, fieldValue);
    }

    private static final OfByte bsoLAYOUT = (OfByte)LAYOUT.select(groupElement("bso"));

    private static final long bso$OFFSET = 7;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char bso
     * }
     */
    public byte bso() {
        return struct.get(bsoLAYOUT, bso$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * char bso
     * }
     */
    public void bso(byte fieldValue) {
        struct.set(bsoLAYOUT, bso$OFFSET, fieldValue);
    }

    private static final OfDouble mean_absorptionLAYOUT = (OfDouble)LAYOUT.select(groupElement("mean_absorption"));

    private static final long mean_absorption$OFFSET = 8;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double mean_absorption
     * }
     */
    public double mean_absorption() {
        return struct.get(mean_absorptionLAYOUT, mean_absorption$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double mean_absorption
     * }
     */
    public void mean_absorption(double fieldValue) {
        struct.set(mean_absorptionLAYOUT, mean_absorption$OFFSET, fieldValue);
    }

    private static final OfShort offsetLAYOUT = (OfShort)LAYOUT.select(groupElement("offset"));

    private static final long offset$OFFSET = 16;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * short offset
     * }
     */
    public short offset() {
        return struct.get(offsetLAYOUT, offset$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * short offset
     * }
     */
    public void offset(short fieldValue) {
        struct.set(offsetLAYOUT, offset$OFFSET, fieldValue);
    }

    private static final OfShort scaleLAYOUT = (OfShort)LAYOUT.select(groupElement("scale"));

    private static final long scale$OFFSET = 18;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * short scale
     * }
     */
    public short scale() {
        return struct.get(scaleLAYOUT, scale$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * short scale
     * }
     */
    public void scale(short fieldValue) {
        struct.set(scaleLAYOUT, scale$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    private static final long spare$OFFSET = 20;

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
