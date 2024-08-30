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
public final class SeabeamSpecific extends StructClass {

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_SHORT.withName("EclipseTime")
    ).withName("t_gsfSeabeamSpecific");

    public SeabeamSpecific(MemorySegment struct) {
        super(struct);
    }

    public SeabeamSpecific(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfShort EclipseTimeLAYOUT = (OfShort)LAYOUT.select(groupElement("EclipseTime"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * unsigned short EclipseTime
     * }
     */
    public static final OfShort EclipseTimeLAYOUT() {
        return EclipseTimeLAYOUT;
    }

    private static final long EclipseTime$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * unsigned short EclipseTime
     * }
     */
    public static final long EclipseTime$offset() {
        return EclipseTime$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short EclipseTime
     * }
     */
    public static short EclipseTime(MemorySegment struct) {
        return struct.get(EclipseTimeLAYOUT, EclipseTime$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short EclipseTime
     * }
     */
    public static void EclipseTime(MemorySegment struct, short fieldValue) {
        struct.set(EclipseTimeLAYOUT, EclipseTime$OFFSET, fieldValue);
    }

}

