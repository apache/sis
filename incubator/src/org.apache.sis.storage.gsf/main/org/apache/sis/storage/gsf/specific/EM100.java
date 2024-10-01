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
import org.apache.sis.storage.gsf.GSF;
import org.apache.sis.storage.gsf.StructClass;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class EM100 extends StructClass {
    private static final OfDouble LAYOUT_SHIP_PITCH;
    private static final OfDouble LAYOUT_TRANSDUCER_PITCH;
    private static final OfInt LAYOUT_MODE;
    private static final OfInt LAYOUT_POWER;
    private static final OfInt LAYOUT_ATTENUATION;
    private static final OfInt LAYOUT_TVG;
    private static final OfInt LAYOUT_PULSE_LENGTH;
    private static final OfInt LAYOUT_COUNTER;

    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        LAYOUT_SHIP_PITCH = GSF.C_DOUBLE.withName("ship_pitch"),
        LAYOUT_TRANSDUCER_PITCH = GSF.C_DOUBLE.withName("transducer_pitch"),
        LAYOUT_MODE = GSF.C_INT.withName("mode"),
        LAYOUT_POWER = GSF.C_INT.withName("power"),
        LAYOUT_ATTENUATION = GSF.C_INT.withName("attenuation"),
        LAYOUT_TVG = GSF.C_INT.withName("tvg"),
        LAYOUT_PULSE_LENGTH = GSF.C_INT.withName("pulse_length"),
        LAYOUT_COUNTER = GSF.C_INT.withName("counter")
    ).withName("t_gsfEM100Specific");

    public EM100(MemorySegment struct) {
        super(struct);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double ship_pitch
     * }
     */
    public double getShipPitch() {
        return struct.get(LAYOUT_SHIP_PITCH, 0);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double transducer_pitch
     * }
     */
    public double getTransducerPitch() {
        return struct.get(LAYOUT_TRANSDUCER_PITCH, 8);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int mode
     * }
     */
    public int getMode() {
        return struct.get(LAYOUT_MODE, 16);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int power
     * }
     */
    public int getPower() {
        return struct.get(LAYOUT_POWER, 20);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int attenuation
     * }
     */
    public int getAttenuation() {
        return struct.get(LAYOUT_ATTENUATION, 24);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int tvg
     * }
     */
    public int getTvg() {
        return struct.get(LAYOUT_TVG, 28);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int pulse_length
     * }
     */
    public int getPulseLength() {
        return struct.get(LAYOUT_PULSE_LENGTH, 32);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int counter
     * }
     */
    public int getCounter() {
        return struct.get(LAYOUT_COUNTER, 36);
    }
}
