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

import static java.lang.foreign.ValueLayout.*;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Comment extends StructClass{

    private static final GroupLayout LAYOUT_COMMENT_TIME;
    private static final OfInt LAYOUT_COMMENT_LENGTH;
    private static final AddressLayout LAYOUT_COMMENT;
    public static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        LAYOUT_COMMENT_TIME = TimeSpec.LAYOUT.withName("comment_time"),
        LAYOUT_COMMENT_LENGTH = GSF.C_INT.withName("comment_length"),
        MemoryLayout.paddingLayout(4),
        LAYOUT_COMMENT = GSF.C_POINTER.withName("comment")
    ).withName("t_gsfComment");

    public Comment(MemorySegment struct) {
        super(struct);
    }

    public Comment(SegmentAllocator allocator) {
        super(allocator);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    public TimeSpec getCommentTime() {
        return new TimeSpec(struct.asSlice(0, LAYOUT_COMMENT_TIME.byteSize()));
    }

    public int getCommentLength() {
        return struct.get(LAYOUT_COMMENT_LENGTH, 16);
    }

    public String getComment() {
        return struct.get(LAYOUT_COMMENT, 24).getString(0);
    }
}