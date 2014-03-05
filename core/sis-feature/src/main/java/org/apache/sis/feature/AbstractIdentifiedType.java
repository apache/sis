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
package org.apache.sis.feature;

import java.io.Serializable;
import org.opengis.util.GenericName;
import org.apache.sis.util.ArgumentChecks;

// Related to JDK7
import java.util.Objects;


/**
 * Identification and description information inherited by property types and feature types.
 *
 * <div class="warning"><b>Warning:</b>
 * This class is expected to implement a GeoAPI {@code IdentifiedType} interface in a future version.
 * When such interface will be available, most references to {@code AbstractIdentifiedType} in the API
 * will be replaced by references to the {@code IdentifiedType} interface.</div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public class AbstractIdentifiedType implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 277130188958446740L;

    /**
     * The name of this type.
     */
    private final GenericName name;

    /**
     * Creates a type of the given name.
     *
     * @param name The name of this type.
     */
    protected AbstractIdentifiedType(final GenericName name) {
        ArgumentChecks.ensureNonNull("name", name);
        this.name = name;
    }

    /**
     * Returns the name of this type.
     * The namespace can be either explicit
     * ({@linkplain org.apache.sis.util.iso.DefaultScopedName scoped name}) or implicit
     * ({@linkplain org.apache.sis.util.iso.DefaultLocalName local name}).
     *
     * <p>For {@linkplain DefaultFeatureType feature types}, the name is mandatory and shall be unique
     * in the unit processing the data (e.g. a {@link org.apache.sis.storage.DataStore} reading a file).</p>
     *
     * @return The type name.
     */
    public GenericName getName() {
        return name;
    }

    /**
     * Returns a hash code value for this type.
     *
     * @return The hash code for this type.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    /**
     * Compares this type with the given object for equality.
     *
     * @param  obj The object to compare with this type.
     * @return {@code true} if the given object is equals to this type.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj != null && getClass() != obj.getClass()) {
            return Objects.equals(name, ((AbstractIdentifiedType) obj).name);
        }
        return false;
    }
}
