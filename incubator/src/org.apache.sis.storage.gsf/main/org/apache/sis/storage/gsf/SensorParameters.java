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

import java.lang.invoke.*;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.MemoryLayout.PathElement.*;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class SensorParameters extends StructClass {

    private static final GroupLayout LAYOUT_PARAM_TIME;
    private static final OfInt LAYOUT_NUMBER_PARAMETERS;
    private static final SequenceLayout LAYOUT_PARAM_SIZE;
    private static final SequenceLayout LAYOUT_PARAM;
    private static final long param$OFFSET = 280;

    static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        LAYOUT_PARAM_TIME = TimeSpec.LAYOUT.withName("param_time"),
        LAYOUT_NUMBER_PARAMETERS = GSF.C_INT.withName("number_parameters"),
        LAYOUT_PARAM_SIZE = MemoryLayout.sequenceLayout(128, GSF.C_SHORT).withName("param_size"),
        MemoryLayout.paddingLayout(4),
        LAYOUT_PARAM = MemoryLayout.sequenceLayout(128, GSF.C_POINTER).withName("param")
    ).withName("t_gsfSensorParameters");

    SensorParameters(MemorySegment struct) {
        super(struct);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * struct timespec param_time
     * }
     */
    public TimeSpec getParamTime() {
        return new TimeSpec(struct.asSlice(0, LAYOUT_PARAM_TIME.byteSize()));
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * int number_parameters
     * }
     */
    public int getNumberParameters() {
        return struct.get(LAYOUT_NUMBER_PARAMETERS, 16);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * short param_size[128]
     * }
     */
    public short[] getParamSize() {
        return getShorts(20, getNumberParameters());
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char *param[128]
     * }
     */
    public String[] getParam() {
        final short[] sizes = getParamSize();
        final String[] params = new String[sizes.length];
        final VarHandle handle = LAYOUT_PARAM.varHandle(sequenceElement());
        for (int i = 0; i < sizes.length; i++) {
            MemorySegment segment = (MemorySegment) handle.get(struct, 0L, i);
            params[i] = segment.getString(0);
        }
        return params;
    }
}
