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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.UnsupportedEncodingException;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.base.ColourInformation;
import org.apache.sis.storage.isobmff.base.ItemPropertyContainer;


/**
 * Describes two or more components with the same type.
 * For example, two monochrome components representing different portions of the electromagnetic spectrum.
 *
 * <h4>Container</h4>
 * The container can be a {@link ItemPropertyContainer} box.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class ComponentDefinition extends Box {
    /**
     * Numerical representation of the {@code "cmpd"} box type.
     */
    public static final int BOXTYPE = ((((('c' << 8) | 'm') << 8) | 'p') << 8) | 'd';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * How pixel data should be displayed. Values are either instance of {@link ComponentType}, {@link URI},
     * {@link String} (when the <abbr>URI</abbr> cannot be parsed) or {@link Integer}, in preference order.
     * In the latter case, the integer value is the user-defined component type.
     *
     * @see Component#type
     * @see ColourInformation
     */
    public final Object[] componentTypes;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws NegativeArraySizeException if an unsigned integer exceeds the capacity of 32-bits signed integers.
     */
    public ComponentDefinition(final Reader reader) throws IOException, UnsupportedEncodingException {
        final ChannelDataInput input = reader.input;
        final int count = input.readInt();
        componentTypes = new Object[count];
        for (int i=0; i<count; i++) {
            final short type = input.readShort();
            Object value;
            if (type >= 0) {
                value = ComponentType.valueOf(type);
            } else {
                final String uri = reader.readNullTerminatedString(false);
                if (uri != null) try {
                    value = new URI(uri);
                    value = reader.unique(value);
                } catch (URISyntaxException e) {
                    reader.cannotParse(e, uri, true);
                    value = uri;
                } else {
                    value = Short.toUnsignedInt(type);
                }
            }
            componentTypes[i] = value;
        }
    }
}
