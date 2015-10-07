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
package org.apache.sis.internal.jaxb.gco;

import java.util.ArrayList;
import java.util.Collection;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.quality.Result;
import org.apache.sis.xml.IdentifiedObject;
import org.apache.sis.xml.IdentifierMap;
import org.apache.sis.internal.jaxb.ModifiableIdentifierMap;


/**
 * A dummy {@link PropertyType} implementation, as a wrapper for instances of the
 * GeoAPI {@link Result} interface.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final strictfp class PropertyTypeMock extends PropertyType<PropertyTypeMock, Result> {
    /**
     * Creates a new adapter. Only the {@link #marshal(Object)}
     * method shall be invoked on that new instance.
     */
    PropertyTypeMock() {
    }

    /**
     * Creates a new wrapper around the given metadata.
     * This constructor is for the {@link #wrap} method only.
     */
    private PropertyTypeMock(final Result metadata) {
        super(metadata);
    }

    /**
     * Invoked by {@link #marshal(Object)} for wrapping the given pseudo-metadata value.
     * If this {@code PropertyTypeMock} class was not for testing purpose only,
     * this method would be invoked by JAXB at marshalling time.
     *
     * @param  metadata The pseudo-metadata element to wrap.
     * @return A {@code PropertyType} wrapping the given the metadata element.
     */
    @Override
    protected PropertyTypeMock wrap(final Result metadata) {
        return new PropertyTypeMock(metadata);
    }

    /**
     * The metadata interface implemented by the objects wrapped by {@code PropertyTypeMock}.
     */
    @Override
    protected Class<Result> getBoundType() {
        return Result.class;
    }

    /**
     * A dummy implementation of the {@link Result} interface, to be used together with {@link PropertyTypeMock}.
     */
    static final class Value implements IdentifiedObject, Result {
        /** All identifiers associated with this metadata. */
        private final Collection<Identifier> identifiers;

        /** A view of the identifiers as a map. */
        private final IdentifierMap map;

        /** Creates a new instance with initially no identifier. */
        Value() {
            identifiers = new ArrayList<Identifier>();
            map = new ModifiableIdentifierMap(identifiers);
        }

        /** Returns the identifiers as a modifiable list. */
        @Override public Collection<? extends Identifier> getIdentifiers() {
            return identifiers;
        }

        /** Returns a view of the identifiers as a map. */
        @Override public IdentifierMap getIdentifierMap() {
            return map;
        }
    }
}
