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
import org.apache.sis.storage.gsf.specific.BDB;
import org.apache.sis.storage.gsf.specific.Echotrac;
import org.apache.sis.storage.gsf.specific.MGD77;
import org.apache.sis.storage.gsf.specific.NOSHDB;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SBSensorSpecific extends StructClass {


    private static final GroupLayout LAYOUT_GSFECHOTRAC;
    private static final GroupLayout LAYOUT_GSFBATHY2000;
    private static final GroupLayout LAYOUT_GSFMGD77;
    private static final GroupLayout LAYOUT_GSFBDB;
    private static final GroupLayout LAYOUT_GSFNOSHDB;

    static final GroupLayout LAYOUT = MemoryLayout.unionLayout(
        LAYOUT_GSFECHOTRAC = Echotrac.LAYOUT.withName("gsfEchotracSpecific"),
        LAYOUT_GSFBATHY2000 = Echotrac.LAYOUT.withName("gsfBathy2000Specific"),
        LAYOUT_GSFMGD77 = MGD77.LAYOUT.withName("gsfMGD77Specific"),
        LAYOUT_GSFBDB = BDB.LAYOUT.withName("gsfBDBSpecific"),
        LAYOUT_GSFNOSHDB = NOSHDB.LAYOUT.withName("gsfNOSHDBSpecific")
    ).withName("t_gsfSBSensorSpecific");

    SBSensorSpecific(MemorySegment struct) {
        super(struct);
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
        return struct.asSlice(0, LAYOUT_GSFECHOTRAC.byteSize());
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * t_gsfEchotracSpecific gsfBathy2000Specific
     * }
     */
    public MemorySegment gsfBathy2000Specific() {
        return struct.asSlice(0, LAYOUT_GSFBATHY2000.byteSize());
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * t_gsfMGD77Specific gsfMGD77Specific
     * }
     */
    public MemorySegment gsfMGD77Specific() {
        return struct.asSlice(0, LAYOUT_GSFMGD77.byteSize());
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * t_gsfBDBSpecific gsfBDBSpecific
     * }
     */
    public MemorySegment gsfBDBSpecific() {
        return struct.asSlice(0, LAYOUT_GSFBDB.byteSize());
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * t_gsfNOSHDBSpecific gsfNOSHDBSpecific
     * }
     */
    public MemorySegment gsfNOSHDBSpecific() {
        return struct.asSlice(0, LAYOUT_GSFNOSHDB.byteSize());
    }
}
