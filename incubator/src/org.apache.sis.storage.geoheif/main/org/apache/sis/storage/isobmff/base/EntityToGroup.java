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
package org.apache.sis.storage.isobmff.base;

import java.io.IOException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.UnsupportedVersionException;


/**
 * An item in a group list.
 *
 * <h4>Container</h4>
 * The container can be a {@link GroupList} box.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public abstract class EntityToGroup extends FullBox {
    /**
     * Unique identifier assigned to the particular grouping.
     * Should not be equal to any other {@code groupID} or {@code itemID}.
     */
    public final int groupID;

    /**
     * Identifiers of items in the group.
     */
    public final int[] entityID;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws UnsupportedVersionException if the box version is unsupported.
     * @throws NegativeArraySizeException if an unsigned integer exceeds the capacity of 32-bits signed integers.
     */
    public EntityToGroup(final Reader reader) throws IOException, UnsupportedVersionException {
        super(reader);
        requireVersionZero();
        final ChannelDataInput input = reader.input;
        groupID  = input.readInt();
        entityID = input.readInts(input.readInt());
    }
}
