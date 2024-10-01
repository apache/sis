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
public final class SBAmp extends StructClass {
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_CHAR.withName("hour"),
        GSF.C_CHAR.withName("minute"),
        GSF.C_CHAR.withName("second"),
        GSF.C_CHAR.withName("hundredths"),
        GSF.C_INT.withName("block_number"),
        GSF.C_SHORT.withName("avg_gate_depth"),
        MemoryLayout.paddingLayout(2)
    ).withName("t_gsfSBAmpSpecific");

    public SBAmp(MemorySegment struct) {
        super(struct);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfByte hourLAYOUT = (OfByte)LAYOUT.select(groupElement("hour"));

    private static final long hour$OFFSET = 0;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char hour
     * }
     */
    public byte hour() {
        return struct.get(hourLAYOUT, hour$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char hour
     * }
     */
    public void hour(byte fieldValue) {
        struct.set(hourLAYOUT, hour$OFFSET, fieldValue);
    }

    private static final OfByte minuteLAYOUT = (OfByte)LAYOUT.select(groupElement("minute"));

    private static final long minute$OFFSET = 1;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char minute
     * }
     */
    public byte minute() {
        return struct.get(minuteLAYOUT, minute$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char minute
     * }
     */
    public void minute(byte fieldValue) {
        struct.set(minuteLAYOUT, minute$OFFSET, fieldValue);
    }

    private static final OfByte secondLAYOUT = (OfByte)LAYOUT.select(groupElement("second"));

    private static final long second$OFFSET = 2;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char second
     * }
     */
    public byte second() {
        return struct.get(secondLAYOUT, second$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char second
     * }
     */
    public void second(byte fieldValue) {
        struct.set(secondLAYOUT, second$OFFSET, fieldValue);
    }

    private static final OfByte hundredthsLAYOUT = (OfByte)LAYOUT.select(groupElement("hundredths"));

    private static final long hundredths$OFFSET = 3;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char hundredths
     * }
     */
    public byte hundredths() {
        return struct.get(hundredthsLAYOUT, hundredths$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char hundredths
     * }
     */
    public void hundredths(byte fieldValue) {
        struct.set(hundredthsLAYOUT, hundredths$OFFSET, fieldValue);
    }

    private static final OfInt block_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("block_number"));

    private static final long block_number$OFFSET = 4;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned int block_number
     * }
     */
    public int block_number() {
        return struct.get(block_numberLAYOUT, block_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned int block_number
     * }
     */
    public void block_number(int fieldValue) {
        struct.set(block_numberLAYOUT, block_number$OFFSET, fieldValue);
    }

    private static final OfShort avg_gate_depthLAYOUT = (OfShort)LAYOUT.select(groupElement("avg_gate_depth"));

    private static final long avg_gate_depth$OFFSET = 8;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * short avg_gate_depth
     * }
     */
    public short avg_gate_depth() {
        return struct.get(avg_gate_depthLAYOUT, avg_gate_depth$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * short avg_gate_depth
     * }
     */
    public void avg_gate_depth(short fieldValue) {
        struct.set(avg_gate_depthLAYOUT, avg_gate_depth$OFFSET, fieldValue);
    }
}
