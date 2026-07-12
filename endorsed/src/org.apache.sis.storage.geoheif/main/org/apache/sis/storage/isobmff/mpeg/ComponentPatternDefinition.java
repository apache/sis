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
 * distributed under the License is distributed on an "AS IS" BASIS,z
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.isobmff.mpeg;

import java.net.URI;
import java.io.IOException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.UnsupportedVersionException;


/**
 * Describes filter array patterns such as Bayer. Shall be present if and only if a
 * {@link ComponentDefinition} box is present and contains {@link ComponentType#FILTER}.
 *
 * <h4>Container</h4>
 * The container can be a {@link ItemPropertyContainer} box.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class ComponentPatternDefinition extends FullBox {
    /**
     * Numerical representation of the {@code "cpat"} box type.
     */
    public static final int BOXTYPE = ((((('c' << 8) | 'p') << 8) | 'a') << 8) | 't';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * The type of the component on which a gain is applied.
     * The value is an instance of {@link ComponentType}, {@link URI} or {@link Integer}.
     */
    public final Object[][] patternComponent;

    /**
     * The gain to be applied on the component.
     */
    public final float[][] patternComponentGain;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @param  defs    definition of components, or {@code null} if none.
     * @throws IOException if an error occurred while reading the payload.
     * @throws UnsupportedVersionException if the box version is unsupported.
     */
    public ComponentPatternDefinition(final Reader reader, final ComponentDefinition defs)
            throws IOException, UnsupportedVersionException
    {
        super(reader);
        requireVersionZero();
        final ChannelDataInput input = reader.input;
        final int width  = input.readUnsignedShort();
        final int height = input.readUnsignedShort();
        patternComponent = new ComponentType[height][width];
        patternComponentGain = new float[height][width];
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                final int index = input.readInt();
                if (defs != null) {
                    patternComponent[y][x] = defs.componentTypes[index];
                }
                patternComponentGain[y][x] = input.readFloat();
            }
        }
    }
}
