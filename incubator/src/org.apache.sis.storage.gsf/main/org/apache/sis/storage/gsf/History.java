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

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class History extends StructClass {

    private static final GroupLayout LAYOUT_HISTORY_TIME;
    private static final SequenceLayout LAYOUT_HOST_NAME;
    private static final SequenceLayout LAYOUT_OPERATOR_NAME;
    private static final AddressLayout LAYOUT_COMMAND_LINE;
    private static final AddressLayout LAYOUT_COMMENT;
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        LAYOUT_HISTORY_TIME = TimeSpec.LAYOUT.withName("history_time"),
        LAYOUT_HOST_NAME = MemoryLayout.sequenceLayout(65, GSF.C_CHAR).withName("host_name"),
        LAYOUT_OPERATOR_NAME = MemoryLayout.sequenceLayout(65, GSF.C_CHAR).withName("operator_name"),
        MemoryLayout.paddingLayout(6),
        LAYOUT_COMMAND_LINE = GSF.C_POINTER.withName("command_line"),
        LAYOUT_COMMENT = GSF.C_POINTER.withName("comment")
    ).withName("t_gsfHistory");

    public History(MemorySegment struct) {
        super(struct);
    }

    public History(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * struct timespec history_time
     * }
     */
    public TimeSpec getHistoryTime() {
        return new TimeSpec(struct.asSlice(0, LAYOUT_HISTORY_TIME.byteSize()));
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char host_name[65]
     * }
     */
    public String getHostName() {
        return new String(getBytes(16, 65));
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char operator_name[65]
     * }
     */
    public String getOperatorName() {
        return new String(getBytes(81, 65));
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char *command_line
     * }
     */
    public MemorySegment getCommandLine(MemorySegment struct) {
        return struct.get(LAYOUT_COMMAND_LINE, 152);
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * char *comment
     * }
     */
    public MemorySegment comment(MemorySegment struct) {
        return struct.get(LAYOUT_COMMENT, 160);
    }

}

