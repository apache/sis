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
package org.apache.sis.internal.simple;

import java.io.Serializable;
import org.opengis.util.Type;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.util.TypeName;


/**
 * A simple attribute type containing only a name and a class of values.
 * Such simple type are suitable for use in ISO 19103 {@link org.opengis.util.RecordType}
 * in addition to ISO 19109 {@link org.opengis.feature.FeatureType}.
 *
 * @param <V> The type of attribute value.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final class SimpleAttributeType<V> implements Type, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4130729627352535488L;

    /**
     * The name for this attribute type.
     */
    private final TypeName name;

    /**
     * The class of value for attributes of this type.
     */
    private final Class<V> valueClass;

    /**
     * Creates a new attribute type for the given name and class of values.
     *
     * @param name       The name for this attribute type (shall not be null).
     * @param valueClass The class of value for attributes of this type (shall not be null).
     */
    public SimpleAttributeType(final TypeName name, final Class<V> valueClass) {
        this.name       = name;
        this.valueClass = valueClass;
    }

    /**
     * Returns the name of this attribute type (ISO 19109).
     *
     * @return The name of this attribute type.
     */
    public GenericName getName() {
        return name;
    }

    /**
     * Returns the name of this attribute type (ISO 19103).
     *
     * @return The name of this attribute type.
     */
    @Override
    public TypeName getTypeName() {
        return name;
    }

    /**
     * Returns the class of value for attributes of this type.
     *
     * @return The class of value for attributes of this type.
     */
    public Class<V> getValueClass() {
        return valueClass;
    }

    /**
     * Returns 1 as of simple feature definition.
     *
     * @return Always 1.
     */
    public int getMinimumOccurs() {
        return 1;
    }

    /**
     * Returns 1 as of simple feature definition.
     *
     * @return Always 1.
     */
    public int getMaximumOccurs() {
        return 1;
    }

    /**
     * Not used for this simple attribute type.
     *
     * @return Always {@code null}.
     */
    public V getDefaultValue() {
        return null;
    }

    /**
     * Not used for this simple attribute type.
     *
     * @return Always {@code null}.
     */
    public InternationalString getDefinition() {
        return null;
    }

    /**
     * Not used for this simple attribute type.
     *
     * @return Always {@code null}.
     */
    public InternationalString getDesignation() {
        return null;
    }

    /**
     * Not used for this simple attribute type.
     *
     * @return Always {@code null}.
     */
    public InternationalString getDescription() {
        return null;
    }

    /**
     * Returns a hash code value for this type.
     *
     * @return A hash code value.
     */
    @Override
    public int hashCode() {
        return name.hashCode() ^ valueClass.hashCode();
    }

    /**
     * Compares this attribute type with the given object for equality.
     *
     * @param object The object to compare with this attribute type.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof SimpleAttributeType) {
            final SimpleAttributeType<?> other = (SimpleAttributeType<?>) object;
            return name.equals(other.name) && valueClass.equals(other.valueClass);
        }
        return false;
    }

    /**
     * Returns the type name.
     *
     * @return The type name.
     */
    @Override
    public String toString() {
        return name.toFullyQualifiedName().toString();
    }
}
