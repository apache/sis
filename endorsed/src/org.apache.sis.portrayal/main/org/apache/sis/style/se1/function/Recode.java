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

package org.apache.sis.style.se1.function;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;


/**
 * Transformation of discrete values to other values, for example integers to texts or colors to other colors.
 * The function is defined by map items, each one associating a data value to a replacement value.
 * The lookup value is compared to the data values for identity, with the following rules:
 *
 * <ul>
 *   <li>Numbers are compared by their numerical value, regardless of their type.</li>
 *   <li>Colors are compared by their ARGB value. Strings in {@code #RRGGBB} or {@code #AARRGGBB}
 *       format are interpreted as colors.</li>
 *   <li>Other values are compared by their string representation, ignoring leading and trailing spaces.</li>
 * </ul>
 *
 * Lookup values which do not match any data value are mapped to the fallback value.
 *
 * <p>In a {@link org.apache.sis.style.se1.ColorReplacement}, the lookup value is the color of each pixel
 * of an external graphic. This function is available by the name {@code "Recode"} in Apache SIS
 * filter factories. The parameters are the lookup value followed by the data and values of each map item
 * in alternance.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @param <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 */
public final class Recode<R> extends AbstractLookupFunction<R,Object> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1580297003413390520L;

    /**
     * The name of this function.
     */
    static final String NAME = "Recode";

    /**
     * The map items.
     */
    private final List<MapItem<R>> mapItems;

    /**
     * Creates a new recode function.
     *
     * @param  lookupValue    the value to recode, or {@code null} for {@code "Rasterdata"}.
     * @param  mapItems       the data values and their replacement values.
     * @param  fallbackValue  value to use when the lookup value does not match any data, or {@code null} if none.
     */
    public Recode(final Expression<R,?> lookupValue, final List<MapItem<R>> mapItems, final Expression<R,?> fallbackValue) {
        super(NAME, Object.class, lookupValue, fallbackValue);
        this.mapItems = List.copyOf(mapItems);
    }

    /**
     * Returns the data values and their replacement values.
     *
     * @return the map items.
     */
    public List<MapItem<R>> getMapItems() {
        return mapItems;
    }

    /**
     * Returns the parameters of this function: the lookup value, then the data and value of each map item
     * in alternance. The fallback value is not included.
     *
     * @return the function parameters.
     */
    @Override
    public List<Expression<R,?>> getParameters() {
        final List<Expression<R,?>> parameters = new ArrayList<>(mapItems.size() * 2 + 1);
        parameters.add(lookupValue);
        for (MapItem<R> item : mapItems) {
            parameters.add(item.getData());
            parameters.add(item.getValue());
        }
        return List.copyOf(parameters);
    }

    @Override
    Object[] properties() {
        return new Object[] {lookupValue, mapItems, fallbackValue};
    }

    /**
     * Returns a function recoding arbitrary lookup values, with map items evaluated once on the given resource.
     * If many map items have the same data value, the first one is used.
     *
     * @param  input  the resource on which to evaluate the map items, may be null for literals.
     * @return a function recoding lookup values.
     */
    @Override
    public Function<Object,Object> bind(final R input) {
        final Map<Object,Object> table = new HashMap<>();
        for (MapItem<R> item : mapItems) {
            table.putIfAbsent(key(item.getData().apply(input)), item.getValue().apply(input));
        }
        final Object fallback = fallback(input);
        return (value) -> {
            final Object key = key(value);
            return (key != null && table.containsKey(key)) ? table.get(key) : fallback;
        };
    }

    /**
     * Returns the key to use for comparing the given value with the data values.
     * Numbers are converted to {@link Double}, colors (including strings in hexadecimal format)
     * to {@link Integer} ARGB values, and other values to strings.
     */
    private static Object key(final Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            final double n = ((Number) value).doubleValue();
            return Double.isNaN(n) ? null : n;
        }
        final String text = value.toString().trim();
        if (value instanceof Color || text.startsWith("#")) {
            final Color c = color(value);
            if (c != null) return c.getRGB();
        }
        return text;
    }

    /**
    * An entry of a {@link Recode} function, associating a data value to a value such as a color.
    *
    * @author  Johann Sorel (Geomatys)
    * @param <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
    * @see Recode#getMapItems()
    */
   public static final class MapItem<R> implements Serializable {
       /**
        * For cross-version compatibility.
        */
       private static final long serialVersionUID = -4407183214016823569L;

       /**
        * The data value to recode.
        */
       private final Expression<R,?> data;

       /**
        * The value replacing the data.
        */
       private final Expression<R,?> value;

       /**
        * Creates a new map item.
        *
        * @param  data   the data value to recode, usually a literal.
        * @param  value  the value replacing the data, usually a literal.
        */
       public MapItem(final Expression<R,?> data, final Expression<R,?> value) {
           this.data  = Objects.requireNonNull(data,  "data");
           this.value = Objects.requireNonNull(value, "value");
       }

       /**
        * Returns the data value to recode.
        *
        * @return the data value.
        */
       public Expression<R,?> getData() {
           return data;
       }

       /**
        * Returns the value replacing the data.
        *
        * @return the replacement value.
        */
       public Expression<R,?> getValue() {
           return value;
       }

       /**
        * Compares this item with the given object for equality.
        *
        * @param  obj  the object to compare with this item.
        * @return whether the two objects are equal.
        */
       @Override
       public boolean equals(final Object obj) {
           if (obj instanceof MapItem) {
               final var other = (MapItem<?>) obj;
               return data.equals(other.data) && value.equals(other.value);
           }
           return false;
       }

       /**
        * Returns a hash code value for this item.
        *
        * @return a hash code value.
        */
       @Override
       public int hashCode() {
           return data.hashCode() * 31 + value.hashCode();
       }

       /**
        * Returns a string representation of this item for debugging purposes.
        *
        * @return a string representation of this item.
        */
       @Override
       public String toString() {
           return "(" + data + " → " + value + ')';
       }
   }

}
