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
package org.apache.sis.xml.bind.metadata.replace;

import java.util.Objects;
import org.opengis.util.TypeName;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.sis.metadata.simple.SimpleIdentifiedObject;
import org.apache.sis.metadata.internal.shared.ReferencingServices;
import org.apache.sis.util.ComparisonMode;

// Specific to the main branch:
import java.util.Set;
import javax.measure.Unit;


/**
 * Base class for ISO/OGC parameter classes replaced by {@code ParameterDescriptor} in GeoAPI.
 * GeoAPI tries to provides a single API for the parameter classes defined in various specifications
 * (ISO 19111, ISO 19115, ISO 19157, Web Processing Service).
 * But we still need separated representations at XML (un)marshalling time.
 *
 * <p>Note that this implementation is simple and serves no other purpose than being a container for XML
 * parsing and formatting. For real parameter framework, consider using {@link org.apache.sis.parameter}
 * package instead.</p>
 *
 * @param  <T>  the type of parameter values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class Parameter<T> extends SimpleIdentifiedObject implements ParameterDescriptor<T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1120310941894856951L;

    /**
     * A copy of {@code this} as a fully-implemented parameter descriptor.
     * This is created when first needed for implementation of {@link #createValue()}.
     * Should not be created for other purposes — in particular for implementation of getter methods —
     * because it would create an infinite loop in {@code DefaultParameterDescriptor} copy constructor.
     */
    private transient volatile ParameterDescriptor<T> descriptor;

    /**
     * Creates an initially empty parameter.
     * This constructor is needed by JAXB at unmarshalling time.
     */
    Parameter() {
    }

    /**
     * Creates a parameter with the values of the given descriptor.
     * This is used at marshalling time for converting a generic descriptor
     * to the standard-specific parameter representation defined by subclass.
     *
     * @param  parameter  the parameter to marshal.
     */
    Parameter(final ParameterDescriptor<T> parameter) {
        super(parameter);
        descriptor = parameter;
    }

    /**
     * Returns the name that describes the type of parameter values.
     * The default implementation returns a non-null value only if this class is wrapping
     * another parameter descriptor. Subclasses should override this method for computing
     * a type name if this method returns null.
     *
     * @return the type name of value component(s) in this parameter, or {@code null} if unknown.
     */
    public TypeName getValueType() {
        return ReferencingServices.getInstance().getValueType(descriptor);
    }

    /**
     * Returns the class that describes the type of parameter values.
     * The default implementation returns a non-null value only if this class is wrapping
     * another parameter descriptor. Subclasses should override this method for computing
     * a class if this method returns null.
     *
     * @return the value class inferred from the attribute type, or {@code null} if unknown.
     */
    @Override
    public Class<T> getValueClass() {
        final ParameterDescriptor<T> p = descriptor;
        return (p != null) ? p.getValueClass() : null;
    }

    /**
     * Creates a new instance of {@code ParameterValue}.
     * This method delegates the work to {@link org.apache.sis.parameter.DefaultParameterDescriptor}
     * since this {@code ServiceParameter} class is not a full-featured parameter descriptor implementation.
     *
     * @return a new instance of {@code ParameterValue}.
     */
    @Override
    public final ParameterValue<T> createValue() {
        ParameterDescriptor<T> p;
        synchronized (this) {
            p = descriptor;
            if (p == null) {
                descriptor = p = ReferencingServices.getInstance().toImplementation(this);
            }
        }
        return p.createValue();
    }

    /**
     * Optional properties.
     * @return {@code null}.
     */
    @Override public Set<T>        getValidValues()  {return null;}     // Really null, not an empty set. See method contract.
    @Override public Comparable<T> getMinimumValue() {return null;}
    @Override public Comparable<T> getMaximumValue() {return null;}
    @Override public T             getDefaultValue() {return null;}
    @Override public Unit<?>       getUnit()         {return null;}

    /*
     * Do not redirect getValidValues(), getMinimumValue(), getMaximumValue(), getDefaultValue() or getUnit()
     * in order to keep property values stable before and after the `descriptor` field has been initialized.
     * The `equals(Object)` method assumes that all those methods return null.
     */

    /**
     * Compares this object with the given one for equality. This implementation should be consistent
     * with {@link org.apache.sis.parameter.DefaultParameterDescriptor#equals(Object)} implementation,
     * with the simplification that some {@code Parameter} property values are always null.
     *
     * @param  object  the object to compare with this reference system.
     * @param  mode    the strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public final boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true;
        }
        if (super.equals(object, mode) && object instanceof ParameterDescriptor<?>) {
            final ParameterDescriptor<?> that = (ParameterDescriptor<?>) object;
            if (that.getUnit()         == null &&
                that.getDefaultValue() == null &&
                that.getValueClass()   == getValueClass())
            {
                if (mode.isIgnoringMetadata()) {
                    return Objects.equals(toString(getName()), toString(that.getName()));
                    // super.equals(…) already compared `getName()` in other modes.
                }
                return that.getMinimumOccurs() == getMinimumOccurs() &&
                       that.getMaximumOccurs() == getMaximumOccurs() &&
                       that.getValidValues()   == null &&
                       that.getMinimumValue()  == null &&
                       that.getMaximumValue()  == null;
            }
        }
        return false;
    }

    /**
     * Null-safe string representation of the given identifier, for comparison purpose.
     * We ignore codespace because they cannot be represented in ISO 19139 XML documents.
     */
    private static String toString(final Identifier identifier) {
        return (identifier != null) ? identifier.toString() : null;
    }
}
