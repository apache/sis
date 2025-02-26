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
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.Reader;


/**
 * All the references from one item of a specific type.
 * This class implements both the {@code SingleItemTypeReference} and {@code SingleItemTypeReferenceLarge} cases.
 *
 * <h4>Container</h4>
 * The container can be an {@link ItemReference} box.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public abstract class SingleItemTypeReference extends Box {
    /**
     * The {@code itemID} of the item that refers to other items.
     */
    @Interpretation(Type.IDENTIFIER)
    public final int fromItemID;

    /**
     * The {@code itemID} of the items referred to.
     */
    @Interpretation(Type.IDENTIFIER)
    public final int[] toItemID;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @param  large   whether to read items as 32-bit integers instead of 16-bit integers.
     * @throws IOException if an error occurred while reading the payload.
     */
    public SingleItemTypeReference(final Reader reader, final boolean large) throws IOException {
        final ChannelDataInput input = reader.input;
        if (large) {
            fromItemID = input.readInt();
            toItemID   = input.readInts(input.readUnsignedShort());
        } else {
            fromItemID  = input.readUnsignedShort();
            final int n = input.readUnsignedShort();
            toItemID = new int[n];
            for (int i=0; i<n; i++) {
                toItemID[i] = input.readUnsignedShort();
            }
        }
    }
}
