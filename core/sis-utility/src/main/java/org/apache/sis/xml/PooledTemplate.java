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
package org.apache.sis.xml;

import java.util.Map;
import javax.xml.validation.Schema;
import javax.xml.bind.PropertyException;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.sis.util.resources.Errors;


/**
 * The template to use for {@link PooledMarshaller} and {@link PooledUnmarshaller} initialization.
 * We use this class for parsing {@link XML} property values from the map given by the user right
 * at {@link MarshallerPool} construction time. This allow both to catch errors sooner, and avoid
 * redoing the conversion every time a new (un)marshaller is requested.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class PooledTemplate extends Pooled {
    /**
     * Creates a new template.
     *
     * @param properties The properties to be given to JAXB (un)marshallers, or {@code null} if none.
     * @param internal {@code true} if the JAXB implementation is the one bundled in JDK 6,
     *        or {@code false} if this is the external implementation provided as a JAR file
     *        in the endorsed directory.
     */
    PooledTemplate(final Map<String,?> properties, final boolean internal) throws PropertyException {
        super(internal);
        if (properties != null) {
            for (final Map.Entry<String,?> entry : properties.entrySet()) {
                setProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Indirectly invoked by the constructor (through the {@link #setProperty(String, Object)} method) for storing
     * a property which is not one of the properties defined in the {@link XML} class. This method overwrites the
     * values stored by the super-class, which are only {@code null} because {@link #getStandardProperty(String)}
     * is implemented that way.
     */
    @Override
    void setStandardProperty(final String name, final Object value) {
        if (initialProperties.put(name, value) != null) {
            throw new AssertionError(name); // If non-null, some code has done unexpected changes in the map.
        }
    }

    /**
     * Indirectly invoked by the constructor as a side-effect of {@link #setProperty(String, Object)} implementation
     * in the super-class This method is not of interest to {@code PooledTemplate}. However as a safety, the above
     * {@link #setStandardProperty(String, Object)} method will check that the map contains the value returned here.
     */
    @Override
    Object getStandardProperty(String name) {
        return null;
    }

    /**
     * Remove the given value from the {@link #initialProperties} map.
     * This method is used for values that are handled especially by the {@link MarshallerPool} constructor.
     *
     * <p>Current implementation expects values of type {@code String}, but this may be generalized
     * in a future SIS version if there is a need for that.</p>
     *
     * @param  name The name of the property to remove.
     * @param  defaultValue The default value to return if the given property is not defined in the map.
     * @return The old value of that property, or {@code defaultValue} if the given property was not defined.
     * @throws PropertyException If the given property is not of the expected type.
     */
    String remove(final String name, final String defaultValue) throws PropertyException {
        final Object value = initialProperties.remove(name);
        if (value instanceof String) {
            return (String) value;
        }
        if (value == null) {
            return defaultValue;
        }
        throw new PropertyException(Errors.format(Errors.Keys.IllegalPropertyValueClass_2, name, value.getClass()));
    }

    /**
     * Should never be invoked on {@code PooledTemplate} instances.
     */
    @Override
    protected void reset(final Object key, final Object value) {
        throw new AssertionError();
    }

    /**
     * Should never be invoked on {@code PooledTemplate} instances.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public <A extends XmlAdapter> A getAdapter(final Class<A> type) {
        throw new AssertionError();
    }

    /**
     * Should never be invoked on {@code PooledTemplate} instances.
     */
    @Override
    public Schema getSchema() {
        throw new AssertionError();
    }

    /**
     * Should never be invoked on {@code PooledTemplate} instances.
     */
    @Override
    public ValidationEventHandler getEventHandler() {
        throw new AssertionError();
    }
}
