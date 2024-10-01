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
public final class EMPUStatus extends StructClass {
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        GSF.C_DOUBLE.withName("pu_cpu_load"),
        GSF.C_SHORT.withName("sensor_status"),
        MemoryLayout.paddingLayout(2),
        GSF.C_INT.withName("achieved_port_coverage"),
        GSF.C_INT.withName("achieved_stbd_coverage"),
        MemoryLayout.paddingLayout(4),
        GSF.C_DOUBLE.withName("yaw_stabilization"),
        MemoryLayout.sequenceLayout(16, GSF.C_CHAR).withName("spare")
    ).withName("t_gsfEMPUStatus");

    public EMPUStatus(MemorySegment struct) {
        super(struct);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    private static final OfDouble pu_cpu_loadLAYOUT = (OfDouble)LAYOUT.select(groupElement("pu_cpu_load"));

    private static final long pu_cpu_load$OFFSET = 0;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double pu_cpu_load
     * }
     */
    public double pu_cpu_load() {
        return struct.get(pu_cpu_loadLAYOUT, pu_cpu_load$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double pu_cpu_load
     * }
     */
    public void pu_cpu_load(double fieldValue) {
        struct.set(pu_cpu_loadLAYOUT, pu_cpu_load$OFFSET, fieldValue);
    }

    private static final OfShort sensor_statusLAYOUT = (OfShort)LAYOUT.select(groupElement("sensor_status"));

    private static final long sensor_status$OFFSET = 8;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned short sensor_status
     * }
     */
    public short sensor_status() {
        return struct.get(sensor_statusLAYOUT, sensor_status$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned short sensor_status
     * }
     */
    public void sensor_status(short fieldValue) {
        struct.set(sensor_statusLAYOUT, sensor_status$OFFSET, fieldValue);
    }

    private static final OfInt achieved_port_coverageLAYOUT = (OfInt)LAYOUT.select(groupElement("achieved_port_coverage"));

    private static final long achieved_port_coverage$OFFSET = 12;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int achieved_port_coverage
     * }
     */
    public int achieved_port_coverage() {
        return struct.get(achieved_port_coverageLAYOUT, achieved_port_coverage$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int achieved_port_coverage
     * }
     */
    public void achieved_port_coverage(int fieldValue) {
        struct.set(achieved_port_coverageLAYOUT, achieved_port_coverage$OFFSET, fieldValue);
    }

    private static final OfInt achieved_stbd_coverageLAYOUT = (OfInt)LAYOUT.select(groupElement("achieved_stbd_coverage"));

    private static final long achieved_stbd_coverage$OFFSET = 16;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int achieved_stbd_coverage
     * }
     */
    public int achieved_stbd_coverage() {
        return struct.get(achieved_stbd_coverageLAYOUT, achieved_stbd_coverage$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * int achieved_stbd_coverage
     * }
     */
    public void achieved_stbd_coverage(int fieldValue) {
        struct.set(achieved_stbd_coverageLAYOUT, achieved_stbd_coverage$OFFSET, fieldValue);
    }

    private static final OfDouble yaw_stabilizationLAYOUT = (OfDouble)LAYOUT.select(groupElement("yaw_stabilization"));

    private static final long yaw_stabilization$OFFSET = 24;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * double yaw_stabilization
     * }
     */
    public double yaw_stabilization() {
        return struct.get(yaw_stabilizationLAYOUT, yaw_stabilization$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * double yaw_stabilization
     * }
     */
    public void yaw_stabilization(double fieldValue) {
        struct.set(yaw_stabilizationLAYOUT, yaw_stabilization$OFFSET, fieldValue);
    }

    private static final SequenceLayout spareLAYOUT = (SequenceLayout)LAYOUT.select(groupElement("spare"));

    private static final long spare$OFFSET = 32;

    /**
     * Getter for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public MemorySegment spare() {
        return struct.asSlice(spare$OFFSET, spareLAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public void spare(MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, spare$OFFSET, spareLAYOUT.byteSize());
    }

    private static final VarHandle spare$ELEM_HANDLE = spareLAYOUT.varHandle(sequenceElement());

    /**
     * Indexed getter for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public byte spare(long index0) {
        return (byte)spare$ELEM_HANDLE.get(struct, 0L, index0);
    }

    /**
     * Indexed setter for field:
     * {@snippet lang=c :
     * unsigned char spare[16]
     * }
     */
    public void spare(long index0, byte fieldValue) {
        spare$ELEM_HANDLE.set(struct, 0L, index0, fieldValue);
    }
}
