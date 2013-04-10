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
package org.apache.sis.measure;

import java.io.InvalidObjectException;
import java.text.Format;


/**
 * Base class of format fields.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
class FormatField extends Format.Field {
    /**
     * Serial number for cross-version compatibility.
     */
    private static final long serialVersionUID = 8152048308355926356L;

    /**
     * A sentinel value for {@link FormattedCharacterIterator} internal usage only,
     * meaning that all attributes shall be taken in account while computing a run range.
     */
    static final FormatField ALL = new FormatField("ALL", 0);

    /**
     * The numeric {@code *_FIELD} value for this constant. This value doesn't need to
     * be serialized, because {@link #readResolve()} will locate the original constant
     * on deserialization.
     */
    final transient int field;

    /**
     * Creates a new field of the given name.
     *
     * @param name  The name, which shall be identical to the name of the public static constant.
     * @param field The numeric identifier of this field.
     */
    FormatField(final String name, final int field) {
        super(name);
        this.field = field;
    }

    /**
     * Invoked on deserialization for resolving this instance to one of the predefined constants.
     *
     * @return One of the predefined constants.
     * @throws InvalidObjectException If this instance can not be resolved.
     */
    @Override
    protected final Object readResolve() throws InvalidObjectException {
        final Class<?> type = getClass();
        try {
            return type.cast(type.getField(getName()).get(null));
        } catch (Exception cause) { // Many exceptions, including unchecked ones.
            InvalidObjectException e = new InvalidObjectException(cause.toString());
            e.initCause(cause);
            throw e;
        }
    }
}
