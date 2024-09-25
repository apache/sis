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
public final class EM950 extends StructClass {
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("ping_number"),
        GSF.C_INT.withName("mode"),
        GSF.C_INT.withName("ping_quality"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("ship_pitch"),
        GSF.C_DOUBLE.withName("transducer_pitch"),
        GSF.C_DOUBLE.withName("surface_velocity")
    ).withName("t_gsfEM950Specific");

    public EM950(MemorySegment struct) {
        super(struct);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt ping_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_number"));

    private static final long ping_number$OFFSET = 0;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_number
     * }
     */
    public int ping_number() {
        return struct.get(ping_numberLAYOUT, ping_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_number
     * }
     */
    public void ping_number(int fieldValue) {
        struct.set(ping_numberLAYOUT, ping_number$OFFSET, fieldValue);
    }

    private static final OfInt modeLAYOUT = (OfInt)LAYOUT.select(groupElement("mode"));

    private static final long mode$OFFSET = 4;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int mode
     * }
     */
    public int mode() {
        return struct.get(modeLAYOUT, mode$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int mode
     * }
     */
    public void mode(int fieldValue) {
        struct.set(modeLAYOUT, mode$OFFSET, fieldValue);
    }

    private static final OfInt ping_qualityLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_quality"));

    private static final long ping_quality$OFFSET = 8;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_quality
     * }
     */
    public int ping_quality() {
        return struct.get(ping_qualityLAYOUT, ping_quality$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_quality
     * }
     */
    public void ping_quality(int fieldValue) {
        struct.set(ping_qualityLAYOUT, ping_quality$OFFSET, fieldValue);
    }

    private static final OfDouble ship_pitchLAYOUT = (OfDouble)LAYOUT.select(groupElement("ship_pitch"));

    private static final long ship_pitch$OFFSET = 16;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double ship_pitch
     * }
     */
    public double ship_pitch() {
        return struct.get(ship_pitchLAYOUT, ship_pitch$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double ship_pitch
     * }
     */
    public void ship_pitch(double fieldValue) {
        struct.set(ship_pitchLAYOUT, ship_pitch$OFFSET, fieldValue);
    }

    private static final OfDouble transducer_pitchLAYOUT = (OfDouble)LAYOUT.select(groupElement("transducer_pitch"));

    private static final long transducer_pitch$OFFSET = 24;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double transducer_pitch
     * }
     */
    public double transducer_pitch() {
        return struct.get(transducer_pitchLAYOUT, transducer_pitch$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double transducer_pitch
     * }
     */
    public void transducer_pitch(double fieldValue) {
        struct.set(transducer_pitchLAYOUT, transducer_pitch$OFFSET, fieldValue);
    }

    private static final OfDouble surface_velocityLAYOUT = (OfDouble)LAYOUT.select(groupElement("surface_velocity"));

    private static final long surface_velocity$OFFSET = 32;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public double surface_velocity() {
        return struct.get(surface_velocityLAYOUT, surface_velocity$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public void surface_velocity(double fieldValue) {
        struct.set(surface_velocityLAYOUT, surface_velocity$OFFSET, fieldValue);
    }
}
