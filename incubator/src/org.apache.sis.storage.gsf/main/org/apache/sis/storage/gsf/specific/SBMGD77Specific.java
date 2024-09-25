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

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfShort time_zone_corrLAYOUT = (OfShort)LAYOUT.select(groupElement("time_zone_corr"));

    private static final long time_zone_corr$OFFSET = 0;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short time_zone_corr
     * }
     */
    public short time_zone_corr() {
        return struct.get(time_zone_corrLAYOUT, time_zone_corr$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short time_zone_corr
     * }
     */
    public void time_zone_corr(short fieldValue) {
        struct.set(time_zone_corrLAYOUT, time_zone_corr$OFFSET, fieldValue);
    }

    private static final OfShort position_type_codeLAYOUT = (OfShort)LAYOUT.select(groupElement("position_type_code"));

    private static final long position_type_code$OFFSET = 2;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short position_type_code
     * }
     */
    public short position_type_code() {
        return struct.get(position_type_codeLAYOUT, position_type_code$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short position_type_code
     * }
     */
    public void position_type_code(short fieldValue) {
        struct.set(position_type_codeLAYOUT, position_type_code$OFFSET, fieldValue);
    }

    private static final OfShort correction_codeLAYOUT = (OfShort)LAYOUT.select(groupElement("correction_code"));

    private static final long correction_code$OFFSET = 4;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short correction_code
     * }
     */
    public short correction_code() {
        return struct.get(correction_codeLAYOUT, correction_code$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short correction_code
     * }
     */
    public void correction_code(short fieldValue) {
        struct.set(correction_codeLAYOUT, correction_code$OFFSET, fieldValue);
    }

    private static final OfShort bathy_type_codeLAYOUT = (OfShort)LAYOUT.select(groupElement("bathy_type_code"));

    private static final long bathy_type_code$OFFSET = 6;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short bathy_type_code
     * }
     */
    public short bathy_type_code() {
        return struct.get(bathy_type_codeLAYOUT, bathy_type_code$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short bathy_type_code
     * }
     */
    public void bathy_type_code(short fieldValue) {
        struct.set(bathy_type_codeLAYOUT, bathy_type_code$OFFSET, fieldValue);
    }

    private static final OfShort quality_codeLAYOUT = (OfShort)LAYOUT.select(groupElement("quality_code"));

    private static final long quality_code$OFFSET = 8;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short quality_code
     * }
     */
    public short quality_code() {
        return struct.get(quality_codeLAYOUT, quality_code$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short quality_code
     * }
     */
    public void quality_code(short fieldValue) {
        struct.set(quality_codeLAYOUT, quality_code$OFFSET, fieldValue);
    }

    private static final OfDouble travel_timeLAYOUT = (OfDouble)LAYOUT.select(groupElement("travel_time"));

    private static final long travel_time$OFFSET = 16;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double travel_time
     * }
     */
    public double travel_time() {
        return struct.get(travel_timeLAYOUT, travel_time$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double travel_time
     * }
     */
    public void travel_time(double fieldValue) {
        struct.set(travel_timeLAYOUT, travel_time$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    private static final long spare$OFFSET = 24;

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
