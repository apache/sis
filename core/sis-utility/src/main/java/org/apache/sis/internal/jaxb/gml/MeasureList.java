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
package org.apache.sis.internal.jaxb.gml;

import java.util.List;
import java.util.AbstractList;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.XmlAttribute;
import javax.measure.unit.Unit;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.ObjectConverters;


/**
 * XML representation of a sequence of measurement values together with their unit of measure.
 * This is used only at XML marshalling time.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see Measure
 */
@XmlType(name = "MeasureListType")
public final class MeasureList {
    /**
     * The measure values.
     */
    @XmlValue
    public List<Double> values;

    /**
     * The unit of measurement.
     */
    public Unit<?> unit;

    /**
     * Default empty constructor for JAXB. The value is initialized to null,
     * but JAXB will overwrite that value if a XML value is present.
     */
    public MeasureList() {
    }

    /**
     * Creates a list of measures backed by the given array.
     *
     * @param <E>         Compile-time value of {@code elementType}.
     * @param array       The measure values as a Java array.
     * @param elementType The type of elements in the given array. Primitive type shall be replaced by their wrapper.
     * @param unit        The unit of measurement.
     */
    public <E> MeasureList(final Object array, final Class<E> elementType, final Unit<?> unit) {
        this.unit = unit;
        final ObjectConverter<? super E, ? extends Double> converter =
                ObjectConverters.find(elementType, Double.class);
        values = new AbstractList<Double>() {
            @Override public int size() {
                return Array.getLength(array);
            }

            @Override@SuppressWarnings("unchecked")
            public Double get(final int index) {
                return converter.apply((E) Array.get(array, index));
            }
        };
    }

    /**
     * Constructs a string representation of the units.
     *
     * @return A string representation of the unit.
     */
    @XmlAttribute(name = "uom", required = true)
    public String getUOM() {
        return Measure.getUOM(unit, false, false);
    }

    /**
     * Sets the unit of measure. This method is invoked by JAXB at unmarshalling time.
     *
     * @param uom The unit of measure as a string.
     * @throws URISyntaxException If the {@code uom} looks like a URI, but can not be parsed.
     */
    public void setUOM(String uom) throws URISyntaxException {
        final Context context = Context.current();
        unit = Context.converter(context).toUnit(context, uom);
    }

    /**
     * Returns the values as an array.
     *
     * @return The values, or {@code null} if none.
     */
    public double[] toArray() {
        if (values == null) {
            return null;
        }
        final double[] array = new double[values.size()];
        for (int i=0; i<array.length; i++) {
            array[i] = values.get(i);
        }
        return array;
    }
}
