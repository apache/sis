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
public final class EM121ASpecific extends StructClass {
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_INT.withName("ping_number"),
        GSF.C_INT.withName("mode"),
        GSF.C_INT.withName("valid_beams"),
        GSF.C_INT.withName("pulse_length"),
        GSF.C_INT.withName("beam_width"),
        GSF.C_INT.withName("tx_power"),
        GSF.C_INT.withName("tx_status"),
        GSF.C_INT.withName("rx_status"),
        GSF.C_DOUBLE.withName("surface_velocity")
    ).withName("t_gsfEM121ASpecific");

    public EM121ASpecific(MemorySegment struct) {
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

    private static final OfInt valid_beamsLAYOUT = (OfInt)LAYOUT.select(groupElement("valid_beams"));

    private static final long valid_beams$OFFSET = 8;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int valid_beams
     * }
     */
    public int valid_beams() {
        return struct.get(valid_beamsLAYOUT, valid_beams$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int valid_beams
     * }
     */
    public void valid_beams(int fieldValue) {
        struct.set(valid_beamsLAYOUT, valid_beams$OFFSET, fieldValue);
    }

    private static final OfInt pulse_lengthLAYOUT = (OfInt)LAYOUT.select(groupElement("pulse_length"));

    private static final long pulse_length$OFFSET = 12;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int pulse_length
     * }
     */
    public int pulse_length() {
        return struct.get(pulse_lengthLAYOUT, pulse_length$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int pulse_length
     * }
     */
    public void pulse_length(int fieldValue) {
        struct.set(pulse_lengthLAYOUT, pulse_length$OFFSET, fieldValue);
    }

    private static final OfInt beam_widthLAYOUT = (OfInt)LAYOUT.select(groupElement("beam_width"));

    private static final long beam_width$OFFSET = 16;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int beam_width
     * }
     */
    public int beam_width() {
        return struct.get(beam_widthLAYOUT, beam_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int beam_width
     * }
     */
    public void beam_width(int fieldValue) {
        struct.set(beam_widthLAYOUT, beam_width$OFFSET, fieldValue);
    }

    private static final OfInt tx_powerLAYOUT = (OfInt)LAYOUT.select(groupElement("tx_power"));

    private static final long tx_power$OFFSET = 20;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int tx_power
     * }
     */
    public int tx_power() {
        return struct.get(tx_powerLAYOUT, tx_power$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int tx_power
     * }
     */
    public void tx_power(int fieldValue) {
        struct.set(tx_powerLAYOUT, tx_power$OFFSET, fieldValue);
    }

    private static final OfInt tx_statusLAYOUT = (OfInt)LAYOUT.select(groupElement("tx_status"));

    private static final long tx_status$OFFSET = 24;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int tx_status
     * }
     */
    public int tx_status() {
        return struct.get(tx_statusLAYOUT, tx_status$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int tx_status
     * }
     */
    public void tx_status(int fieldValue) {
        struct.set(tx_statusLAYOUT, tx_status$OFFSET, fieldValue);
    }

    private static final OfInt rx_statusLAYOUT = (OfInt)LAYOUT.select(groupElement("rx_status"));

    private static final long rx_status$OFFSET = 28;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int rx_status
     * }
     */
    public int rx_status() {
        return struct.get(rx_statusLAYOUT, rx_status$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int rx_status
     * }
     */
    public void rx_status(int fieldValue) {
        struct.set(rx_statusLAYOUT, rx_status$OFFSET, fieldValue);
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
