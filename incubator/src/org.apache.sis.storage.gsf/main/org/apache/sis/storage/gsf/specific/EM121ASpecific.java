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

    public EM121ASpecific(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfInt ping_numberLAYOUT = (OfInt)LAYOUT.select(groupElement("ping_number"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int ping_number
     * }
     */
    public static final OfInt ping_numberLAYOUT() {
        return ping_numberLAYOUT;
    }

    private static final long ping_number$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int ping_number
     * }
     */
    public static final long ping_number$offset() {
        return ping_number$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int ping_number
     * }
     */
    public static int ping_number(MemorySegment struct) {
        return struct.get(ping_numberLAYOUT, ping_number$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int ping_number
     * }
     */
    public static void ping_number(MemorySegment struct, int fieldValue) {
        struct.set(ping_numberLAYOUT, ping_number$OFFSET, fieldValue);
    }

    private static final OfInt modeLAYOUT = (OfInt)LAYOUT.select(groupElement("mode"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int mode
     * }
     */
    public static final OfInt modeLAYOUT() {
        return modeLAYOUT;
    }

    private static final long mode$OFFSET = 4;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int mode
     * }
     */
    public static final long mode$offset() {
        return mode$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int mode
     * }
     */
    public static int mode(MemorySegment struct) {
        return struct.get(modeLAYOUT, mode$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int mode
     * }
     */
    public static void mode(MemorySegment struct, int fieldValue) {
        struct.set(modeLAYOUT, mode$OFFSET, fieldValue);
    }

    private static final OfInt valid_beamsLAYOUT = (OfInt)LAYOUT.select(groupElement("valid_beams"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int valid_beams
     * }
     */
    public static final OfInt valid_beamsLAYOUT() {
        return valid_beamsLAYOUT;
    }

    private static final long valid_beams$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int valid_beams
     * }
     */
    public static final long valid_beams$offset() {
        return valid_beams$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int valid_beams
     * }
     */
    public static int valid_beams(MemorySegment struct) {
        return struct.get(valid_beamsLAYOUT, valid_beams$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int valid_beams
     * }
     */
    public static void valid_beams(MemorySegment struct, int fieldValue) {
        struct.set(valid_beamsLAYOUT, valid_beams$OFFSET, fieldValue);
    }

    private static final OfInt pulse_lengthLAYOUT = (OfInt)LAYOUT.select(groupElement("pulse_length"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int pulse_length
     * }
     */
    public static final OfInt pulse_lengthLAYOUT() {
        return pulse_lengthLAYOUT;
    }

    private static final long pulse_length$OFFSET = 12;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int pulse_length
     * }
     */
    public static final long pulse_length$offset() {
        return pulse_length$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int pulse_length
     * }
     */
    public static int pulse_length(MemorySegment struct) {
        return struct.get(pulse_lengthLAYOUT, pulse_length$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int pulse_length
     * }
     */
    public static void pulse_length(MemorySegment struct, int fieldValue) {
        struct.set(pulse_lengthLAYOUT, pulse_length$OFFSET, fieldValue);
    }

    private static final OfInt beam_widthLAYOUT = (OfInt)LAYOUT.select(groupElement("beam_width"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int beam_width
     * }
     */
    public static final OfInt beam_widthLAYOUT() {
        return beam_widthLAYOUT;
    }

    private static final long beam_width$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int beam_width
     * }
     */
    public static final long beam_width$offset() {
        return beam_width$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int beam_width
     * }
     */
    public static int beam_width(MemorySegment struct) {
        return struct.get(beam_widthLAYOUT, beam_width$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int beam_width
     * }
     */
    public static void beam_width(MemorySegment struct, int fieldValue) {
        struct.set(beam_widthLAYOUT, beam_width$OFFSET, fieldValue);
    }

    private static final OfInt tx_powerLAYOUT = (OfInt)LAYOUT.select(groupElement("tx_power"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int tx_power
     * }
     */
    public static final OfInt tx_powerLAYOUT() {
        return tx_powerLAYOUT;
    }

    private static final long tx_power$OFFSET = 20;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int tx_power
     * }
     */
    public static final long tx_power$offset() {
        return tx_power$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int tx_power
     * }
     */
    public static int tx_power(MemorySegment struct) {
        return struct.get(tx_powerLAYOUT, tx_power$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int tx_power
     * }
     */
    public static void tx_power(MemorySegment struct, int fieldValue) {
        struct.set(tx_powerLAYOUT, tx_power$OFFSET, fieldValue);
    }

    private static final OfInt tx_statusLAYOUT = (OfInt)LAYOUT.select(groupElement("tx_status"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int tx_status
     * }
     */
    public static final OfInt tx_statusLAYOUT() {
        return tx_statusLAYOUT;
    }

    private static final long tx_status$OFFSET = 24;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int tx_status
     * }
     */
    public static final long tx_status$offset() {
        return tx_status$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int tx_status
     * }
     */
    public static int tx_status(MemorySegment struct) {
        return struct.get(tx_statusLAYOUT, tx_status$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int tx_status
     * }
     */
    public static void tx_status(MemorySegment struct, int fieldValue) {
        struct.set(tx_statusLAYOUT, tx_status$OFFSET, fieldValue);
    }

    private static final OfInt rx_statusLAYOUT = (OfInt)LAYOUT.select(groupElement("rx_status"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * int rx_status
     * }
     */
    public static final OfInt rx_statusLAYOUT() {
        return rx_statusLAYOUT;
    }

    private static final long rx_status$OFFSET = 28;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * int rx_status
     * }
     */
    public static final long rx_status$offset() {
        return rx_status$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int rx_status
     * }
     */
    public static int rx_status(MemorySegment struct) {
        return struct.get(rx_statusLAYOUT, rx_status$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int rx_status
     * }
     */
    public static void rx_status(MemorySegment struct, int fieldValue) {
        struct.set(rx_statusLAYOUT, rx_status$OFFSET, fieldValue);
    }

    private static final OfDouble surface_velocityLAYOUT = (OfDouble)LAYOUT.select(groupElement("surface_velocity"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public static final OfDouble surface_velocityLAYOUT() {
        return surface_velocityLAYOUT;
    }

    private static final long surface_velocity$OFFSET = 32;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public static final long surface_velocity$offset() {
        return surface_velocity$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public static double surface_velocity(MemorySegment struct) {
        return struct.get(surface_velocityLAYOUT, surface_velocity$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double surface_velocity
     * }
     */
    public static void surface_velocity(MemorySegment struct, double fieldValue) {
        struct.set(surface_velocityLAYOUT, surface_velocity$OFFSET, fieldValue);
    }

}

