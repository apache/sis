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
package org.apache.sis.metadata;

import java.util.Set;
import java.util.Collection;
import java.util.Collections;
import java.lang.reflect.Method;
import net.jcip.annotations.Immutable;
import javax.measure.unit.Unit;
import org.opengis.annotation.UML;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.internal.simple.SimpleReferenceIdentifier;

// Related to JDK7
import java.util.Objects;


/**
 * Description of a metadata property inferred from Java reflection.
 * For a given metadata instances (typically an {@link AbstractMetadata} subclasses,
 * but other types are allowed), instances of {@code PropertyDescriptor} are obtained
 * indirectly by the {@link MetadataStandard#asDescriptorMap(Object, KeyNamePolicy,
 * NullValuePolicy)} method.
 *
 * @param <T> The value type, either the method return type if not a collection,
 *            or the type of elements in the collection otherwise.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.05)
 * @version 0.3
 * @module
 *
 * @see MetadataStandard#asDescriptorMap(Object, KeyNamePolicy, NullValuePolicy)
 */
@Immutable
class PropertyDescriptor<T> extends SimpleReferenceIdentifier implements ParameterDescriptor<T> {
    /**
     * For cross-versions compatibility.
     */
    private static final long serialVersionUID = 888961503200860655L;

    /**
     * ISO name of the interface which contain this property.
     *
     * @see #getCodeSpace()
     */
    private final String container;

    /**
     * The value type, either the method return type if not a collection,
     * or the type of elements in the collection otherwise.
     *
     * @see #getValueClass()
     */
    private final Class<T> elementType;

    /**
     * Minimal and maximal occurrences of this property, packed as below:
     *
     * <ul>
     *   <li>Rightmost bit encodes the minimal occurrence.</li>
     *   <li>All other bits encode the maximal occurrence.</li>
     * </ul>
     *
     * @see #getMinimumOccurs()
     * @see #getMaximumOccurs()
     */
    private final byte cardinality;

    /**
     * Specialization of {@link PropertyDescriptor} when the range of legal values is
     * specified by a {@link ValueRange} annotation. Those values must be numeric.
     *
     * <p>Bounded properties are represented by a different class because the requirement
     * for numerical values put additional constraints on the {@code <T>} parameterized type,
     * and because bounded properties are relatively rare so we can avoid to carry a null
     * range fields for the properties that don't need it.</p>
     *
     * @param <T> The numeric value type.
     */
    static final class Bounded<T extends Number & Comparable<T>> extends PropertyDescriptor<T> {
        /** For cross versions compatibility. */
        private static final long serialVersionUID = 7869130373802184834L;

        /** The range of valid values. */
        private final NumberRange<T> range;

        /** Creates a new descriptor for the given range of values. */
        Bounded(final String container, final Class<T> type, final Citation standard,
                final String property, final Method getter, final ValueRange range)
        {
            super(container, type, standard, property, getter);
            this.range = new NumberRange<>(type, range);
        }

        /** Returns the minimum parameter value, assumed inclusive. */
        @Override
        public Comparable<T> getMinimumValue() {
            return range.getMinValue();
        }

        /** Returns the maximum parameter value, assumed inclusive. */
        @Override
        public Comparable<T> getMaximumValue() {
            return range.getMaxValue();
        }

        /** Compares the given object for equality. */
        @Override
        public boolean equals(final Object obj) {
            if (super.equals(obj)) {
                return Objects.equals(range, ((Bounded) obj).range);
            }
            return false;
        }
    }

    /**
     * Creates a new {@code PropertyDescriptor} instance from the annotations on the given
     * getter method. If there is no restriction, then this method returns {@code null}.
     *
     * @param  container   ISO name of the interface which contain this property.
     * @param  elementType The value type, either the method return type if not a collection,
     *                     or the type of elements in the collection otherwise.
     * @param  standard    The international standard that define the property, or {@code null} if none.
     * @param  property    The property name as defined by the international {@linkplain #standard}.
     * @param  getter      The getter method defined in the interface.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    PropertyDescriptor(final String container, final Class<T> elementType,
            final Citation standard, final String property, final Method getter)
    {
        super(standard, property);
        this.container = container;
        this.elementType = elementType;
        final UML uml = getter.getAnnotation(UML.class);
        byte cardinality = 2;
        if (uml != null) {
            switch (uml.obligation()) {
                case MANDATORY:      cardinality = 3; break;  // minOccurs = 1, maxOccurs = 1;
                case FORBIDDEN: this.cardinality = 0; return; // minOccurs = 0, maxOccurs = 0;
            }
        }
        final Class<?> c = getter.getReturnType();
        if (c.isArray() || Collection.class.isAssignableFrom(c)) {
            cardinality |= 0xFE; // Set all bits except the rightmost one, which is left unchanged.
        }
        this.cardinality = cardinality;
    }

    /**
     * Returns the primary name by which this object is identified.
     * This is fixed to {@code this} for more efficient storage of
     * potentially large amount of {@code PropertyDescriptor}s.
     *
     * <p>Users invoke this method in order to get access to the {@link #getCode()},
     * {@link #getCodeSpace()}, {@link #getVersion()} and {@link #getAuthority()} methods.</p>
     */
    @Override
    public final ReferenceIdentifier getName() {
        return this;
    }

    /**
     * Returns the code space of the property name.
     */
    @Override
    public final String getCodeSpace() {
        return container;
    }

    /**
     * An identifier which references elsewhere the object's defining information.
     * The current implementation returns an empty set.
     */
    @Override
    public final Set<ReferenceIdentifier> getIdentifiers() {
        return Collections.emptySet();
    }

    /**
     * An alternative name by which this object is identified.
     * The current implementation returns an empty set.
     * Future implementation may return the JavaBeans property name.
     */
    @Override
    public final Collection<GenericName> getAlias() {
        return Collections.emptySet();
    }

    /**
     * Comments about this property, or {@code null} if none.
     */
    @Override
    public InternationalString getRemarks() {
        return null; // TODO use Types.getDescription
    }

    /**
     * Returns the class that describe the type of the parameter.
     * This is the type given at construction time.
     */
    @Override
    public final Class<T> getValueClass() {
        return elementType;
    }

    /**
     * Returns the default value for the parameter, or {@code null} if none.
     * Current implementation unconditionally return {@code null}.
     */
    @Override
    public T getDefaultValue() {
        return null;
    }

    /**
     * Returns the minimum parameter value, or {@code null} if none.
     * The {@link ValueRange#minimum()} value is assumed inclusive
     * (this is not verified).
     */
    @Override
    public Comparable<T> getMinimumValue() {
        return null;
    }

    /**
     * Returns the minimum parameter value, or {@code null} if none.
     * The {@link ValueRange#maximum()} value is assumed inclusive
     * (this is not verified).
     */
    @Override
    public Comparable<T> getMaximumValue() {
        return null;
    }

    /**
     * Returns the set of allowed values when these are restricted to some finite set.
     * The default implementation returns {@code null} since there is no restriction even
     * on code list (we can not return the set of all code list elements since this list
     * may growth at runtime).
     *
     * However if we want to restrict the set of applicable code list values, we can do
     * that here using {@link org.apache.sis.util.collection.CodeListSet}.
     */
    @Override
    public Set<T> getValidValues() {
        return null;
    }

    /**
     * Returns the unit of measurement. The current implementation returns {@code null}.
     */
    @Override
    public Unit<?> getUnit() {
        return null;
    }

    /**
     * The minimum number of times that values for this parameter group or
     * parameter are required.
     */
    @Override
    public int getMinimumOccurs() {
        return cardinality & 1;
    }

    /**
     * The maximum number of times that values for this parameter group or
     * parameter are required.
     */
    @Override
    public int getMaximumOccurs() {
        return ((int) cardinality) >>> 1;
    }

    @Override
    public ParameterValue<T> createValue() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Compares the given object with this descriptor for equality.
     *
     * @param  obj The object to compare with this descriptor for equality.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (super.equals(obj)) {
            final PropertyDescriptor<?> that = (PropertyDescriptor<?>) obj;
            return (this.cardinality == that.cardinality) &&
                   Objects.equals(this.container,   that.container) &&
                   Objects.equals(this.elementType, that.elementType);
        }
        return false;
    }

    /**
     * Computes a hash code value only from the code space and property name.
     * We don't need to use the other properties, because the fully qualified
     * property name should be a sufficient discriminator.
     */
    @Override
    public final int hashCode() {
        return (container.hashCode() + 31 * code.hashCode()) ^ (int) serialVersionUID;
    }
}
