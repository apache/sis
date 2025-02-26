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
package org.apache.sis.storage.isobmff.image;

import java.util.Locale;
import java.io.IOException;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;
import org.apache.sis.storage.isobmff.UnsupportedVersionException;


/**
 * User-defined name, description and tags.
 * When several instances are associated with the same item or entity group,
 * they represent alternatives possibly expressed in different languages.
 *
 * <h4>Container</h4>
 * The container can be a {@link ItemPropertyContainer} box.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class UserDescription extends FullBox {
    /**
     * Numerical representation of the {@code "udes"} box type.
     */
    public static final int BOXTYPE = ((((('u' << 8) | 'd') << 8) | 'e') << 8) | 's';

    /**
     * Returns the four-character type of this box.
     * This value is fixed to {@link #BOXTYPE}.
     */
    @Override
    public final int type() {
        return BOXTYPE;
    }

    /**
     * RFC 5646 compliant language tag, such as {@code "en-US"} or {@code "fr-FR"}.
     * A null value means that the language is unknown or undefined.
     */
    public final Locale locale;

    /**
     * Human-readable name for the item, or {@code null} if none.
     */
    public final String name;

    /**
     * Human-readable description of the item, or {@code null} if none.
     */
    public final String description;

    /**
     * User-defined tags related to the item, or {@code null} if none.
     */
    public final String[] tags;

    /**
     * Creates a new box and loads the payload from the given reader.
     *
     * @param  reader  the reader from which to read the payload.
     * @throws IOException if an error occurred while reading the payload.
     * @throws UnsupportedVersionException if the box version is unsupported.
     */
    public UserDescription(final Reader reader) throws IOException, UnsupportedVersionException {
        super(reader);
        requireVersionZero();
        String lang = reader.readNullTerminatedString(false);
        locale      = (lang != null) ? Locale.forLanguageTag(lang) : null;
        name        = reader.readNullTerminatedString(false);
        description = reader.readNullTerminatedString(false);
        String cst  = reader.readNullTerminatedString(false);
        tags = (cst != null) ? cst.split("\\s*,\\s*") : null;
    }

    /**
     * Converts node properties to <abbr>ISO</abbr> 19115 metadata.
     *
     * @param  builder  the builder where to set metadata information.
     *
     * @todo build an InternationalString and store somewhere in metadata.
     */
    @Override
    public void metadata(final MetadataBuilder builder) {
    }
}
