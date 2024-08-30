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
public final class EchotracSpecific extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("navigation_error"),
        GSF.C_SHORT.withName("mpp_source"),
        GSF.C_SHORT.withName("tide_source")
    ).withName("t_gsfEchotracSpecific");

    public EchotracSpecific(MemorySegment struct) {
        super(struct);
    }

    public EchotracSpecific(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt navigation_errorLAYOUT = (OfInt)LAYOUT.select(groupElement("navigation_error"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int navigation_error
     * }
     */
    public static final OfInt navigation_errorLAYOUT() {
        return navigation_errorLAYOUT;
    }

    private static final long navigation_error$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int navigation_error
     * }
     */
    public static final long navigation_error$offset() {
        return navigation_error$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int navigation_error
     * }
     */
    public static int navigation_error(MemorySegment struct) {
        return struct.get(navigation_errorLAYOUT, navigation_error$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int navigation_error
     * }
     */
    public static void navigation_error(MemorySegment struct, int fieldValue) {
        struct.set(navigation_errorLAYOUT, navigation_error$OFFSET, fieldValue);
    }

    private static final OfShort mpp_sourceLAYOUT = (OfShort)LAYOUT.select(groupElement("mpp_source"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned short mpp_source
     * }
     */
    public static final OfShort mpp_sourceLAYOUT() {
        return mpp_sourceLAYOUT;
    }

    private static final long mpp_source$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned short mpp_source
     * }
     */
    public static final long mpp_source$offset() {
        return mpp_source$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short mpp_source
     * }
     */
    public static short mpp_source(MemorySegment struct) {
        return struct.get(mpp_sourceLAYOUT, mpp_source$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short mpp_source
     * }
     */
    public static void mpp_source(MemorySegment struct, short fieldValue) {
        struct.set(mpp_sourceLAYOUT, mpp_source$OFFSET, fieldValue);
    }

    private static final OfShort tide_sourceLAYOUT = (OfShort)LAYOUT.select(groupElement("tide_source"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned short tide_source
     * }
     */
    public static final OfShort tide_sourceLAYOUT() {
        return tide_sourceLAYOUT;
    }

    private static final long tide_source$OFFSET = 6;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned short tide_source
     * }
     */
    public static final long tide_source$offset() {
        return tide_source$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short tide_source
     * }
     */
    public static short tide_source(MemorySegment struct) {
        return struct.get(tide_sourceLAYOUT, tide_source$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short tide_source
     * }
     */
    public static void tide_source(MemorySegment struct, short fieldValue) {
        struct.set(tide_sourceLAYOUT, tide_source$OFFSET, fieldValue);
    }

}

