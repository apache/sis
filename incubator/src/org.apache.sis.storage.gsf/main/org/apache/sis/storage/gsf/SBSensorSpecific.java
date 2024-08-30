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
package org.apache.sis.storage.gsf;

import java.lang.foreign.*;
import org.apache.sis.storage.gsf.specific.BDBSpecific;
import org.apache.sis.storage.gsf.specific.EchotracSpecific;
import org.apache.sis.storage.gsf.specific.MGD77Specific;
import org.apache.sis.storage.gsf.specific.NOSHDBSpecific;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SBSensorSpecific extends StructClass {


    private static final GroupLayout LAYOUT_GSFECHOTRACSPECIFIC;
    private static final GroupLayout LAYOUT_GSFBATHY2000SPECIFIC;
    private static final GroupLayout LAYOUT_GSFMGD77SPECIFIC;
    private static final GroupLayout LAYOUT_GSFBDBSPECIFIC;
    private static final GroupLayout LAYOUT_GSFNOSHDBSPECIFIC;

    public static final GroupLayout LAYOUT = MemoryLayout.unionLayout(
        LAYOUT_GSFECHOTRACSPECIFIC = EchotracSpecific.LAYOUT.withName("gsfEchotracSpecific"),
        LAYOUT_GSFBATHY2000SPECIFIC = EchotracSpecific.LAYOUT.withName("gsfBathy2000Specific"),
        LAYOUT_GSFMGD77SPECIFIC = MGD77Specific.LAYOUT.withName("gsfMGD77Specific"),
        LAYOUT_GSFBDBSPECIFIC = BDBSpecific.LAYOUT.withName("gsfBDBSpecific"),
        LAYOUT_GSFNOSHDBSPECIFIC = NOSHDBSpecific.LAYOUT.withName("gsfNOSHDBSpecific")
    ).withName("t_gsfSBSensorSpecific");

    public SBSensorSpecific(MemorySegment struct) {
        super(struct);
    }

    public SBSensorSpecific(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * t_gsfEchotracSpecific gsfEchotracSpecific
     * }
     */
    public MemorySegment gsfEchotracSpecific() {
        return struct.asSlice(0, LAYOUT_GSFECHOTRACSPECIFIC.byteSize());
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * t_gsfEchotracSpecific gsfBathy2000Specific
     * }
     */
    public MemorySegment gsfBathy2000Specific() {
        return struct.asSlice(0, LAYOUT_GSFBATHY2000SPECIFIC.byteSize());
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * t_gsfMGD77Specific gsfMGD77Specific
     * }
     */
    public MemorySegment gsfMGD77Specific() {
        return struct.asSlice(0, LAYOUT_GSFMGD77SPECIFIC.byteSize());
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * t_gsfBDBSpecific gsfBDBSpecific
     * }
     */
    public MemorySegment gsfBDBSpecific() {
        return struct.asSlice(0, LAYOUT_GSFBDBSPECIFIC.byteSize());
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * t_gsfNOSHDBSpecific gsfNOSHDBSpecific
     * }
     */
    public MemorySegment gsfNOSHDBSpecific() {
        return struct.asSlice(0, LAYOUT_GSFNOSHDBSPECIFIC.byteSize());
    }

}

