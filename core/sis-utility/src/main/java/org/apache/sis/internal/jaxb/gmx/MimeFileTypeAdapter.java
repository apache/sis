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
package org.apache.sis.internal.jaxb.gmx;

import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.gco.StringAdapter;
import org.apache.sis.internal.jaxb.gco.CharSequenceAdapter;
import org.apache.sis.internal.jaxb.gco.GO_CharacterString;


/**
 * JAXB adapter wrapping a {@code String} value with a {@code <gmx:MimeFileType>} element.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final class MimeFileTypeAdapter extends StringAdapter {
    /**
     * Empty constructor for JAXB.
     */
    public MimeFileTypeAdapter() {
    }

    /**
     * Converts a MIME type to the object to be marshalled in a XML file or stream.
     * JAXB calls automatically this method at marshalling time.
     *
     * @param  value The MIME type, or {@code null}.
     * @return The wrapper for the given MIME type, or {@code null}.
     */
    @Override
    public GO_CharacterString marshal(final String value) {
        final Context context = Context.current();
        final GO_CharacterString wrapper = CharSequenceAdapter.wrap(context, value, value);
        if (wrapper != null && wrapper.type == 0) {
            if (!Context.isFlagSet(context, Context.SUBSTITUTE_MIMETYPE)) {
                wrapper.type = GO_CharacterString.MIME_TYPE;
            }
            return wrapper;
        }
        return null;
    }
}
