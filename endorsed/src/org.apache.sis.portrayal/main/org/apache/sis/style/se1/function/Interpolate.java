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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;


/**
 * Transformation of continuous values by a function defined on interpolation points.
 * Each point associates a data value to a value, which is a number or a color.
 * Lookup values between two points are interpolated according the {@linkplain #getMode() mode}.
 * Lookup values below the first point or above the last point take the value of that point.
 * Only numeric quantities are allowed for lookup values and data. Lookup values which are missing
 * or not numbers are mapped to the fallback value.
 *
 * <p>The {@linkplain #getMethod() method} specifies whether values are interpolated as numbers or as colors
 * (component by component, including alpha). The computed values are {@link Double} or {@link Color} instances
 * respectively. If the method is not specified, values are interpolated as colors if they are not numbers but
 * are colors (including strings in {@code #RRGGBB} format), and as numbers otherwise.
 * Use {@link #toValueType(Class)} for a more specific expression type.</p>
 *
 * <p>In a {@link org.apache.sis.style.se1.ColorMap}, the lookup value is the raster data ({@code "Rasterdata"})
 * and the values are colors. This function is available by the name {@code "Interpolate"} in Apache SIS
 * filter factories. The parameters are the lookup value followed by the data and values of each interpolation
 * point in alternance, optionally followed by the mode keyword ({@code "linear"}, {@code "cosine"} or
 * {@code "cubic"}) and the method keyword ({@code "numeric"} or {@code "color"}).</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @param <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 */
public final class Interpolate<R> extends AbstractLookupFunction<R,Object> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6213431735566457227L;

    /**
     * The name of this function.
     */
    static final String NAME = "Interpolate";

    /**
     * The interpolation method between two interpolation points.
     */
    public enum Mode {
        /** Linear interpolation. This is the default. */
        LINEAR,

        /** Cosine interpolation, smoother than linear near the interpolation points. */
        COSINE,

        /** Cubic interpolation (Catmull-Rom spline), passing smoothly through all interpolation points. */
        CUBIC
    }

    /**
     * Whether the interpolated values are numbers or colors.
     */
    public enum Method {
        /** Values are interpolated as numbers. */
        NUMERIC,

        /** Values are interpolated as colors, component by component including alpha. */
        COLOR
    }

    /**
     * The interpolation points.
     */
    private final List<InterpolationPoint<R>> interpolationPoints;

    /**
     * The interpolation mode.
     */
    private final Mode mode;

    /**
     * The interpolation method, or {@code null} for determining it from the values.
     */
    private final Method method;

    /**
     * Creates a new interpolation function.
     *
     * @param  lookupValue          the value to interpolate, or {@code null} for {@code "Rasterdata"}.
     * @param  interpolationPoints  the interpolation points, in ascending order of data values.
     * @param  mode                 the interpolation mode, or {@code null} for the default value ({@code LINEAR}).
     * @param  method               the interpolation method, or {@code null} for determining it from the values.
     * @param  fallbackValue        value to use when the lookup value cannot be interpolated, or {@code null} if none.
     */
    public Interpolate(final Expression<R,?> lookupValue,
                       final List<InterpolationPoint<R>> interpolationPoints,
                       final Mode mode, final Method method,
                       final Expression<R,?> fallbackValue)
    {
        super(NAME, Object.class, lookupValue, fallbackValue);
        this.interpolationPoints = List.copyOf(interpolationPoints);
        this.mode   = (mode != null) ? mode : Mode.LINEAR;
        this.method = method;
    }

    /**
     * Returns the interpolation points.
     *
     * @return the interpolation points.
     */
    public List<InterpolationPoint<R>> getInterpolationPoints() {
        return interpolationPoints;
    }

    /**
     * Returns the interpolation mode.
     *
     * @return the interpolation mode.
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Returns whether the interpolated values are numbers or colors.
     *
     * @return the interpolation method, or empty if determined from the values.
     */
    public Optional<Method> getMethod() {
        return Optional.ofNullable(method);
    }

    /**
     * Returns the parameters of this function: the lookup value, then the data and value of each point
     * in alternance, then the mode keyword and the method keyword if specified.
     * The fallback value is not included.
     *
     * @return the function parameters.
     */
    @Override
    public List<Expression<R,?>> getParameters() {
        final List<Expression<R,?>> parameters = new ArrayList<>(interpolationPoints.size() * 2 + 3);
        parameters.add(lookupValue);
        for (InterpolationPoint<R> p : interpolationPoints) {
            parameters.add(p.getData());
            parameters.add(p.getValue());
        }
        parameters.add(new Keyword<>(keyword(mode)));
        if (method != null) {
            parameters.add(new Keyword<>(keyword(method)));
        }
        return List.copyOf(parameters);
    }

    @Override
    Object[] properties() {
        return new Object[] {lookupValue, interpolationPoints, mode, method, fallbackValue};
    }

    /**
     * Returns a function interpolating arbitrary lookup values, with interpolation points evaluated
     * once on the given resource. Points without valid data or value are ignored.
     *
     * @param  input  the resource on which to evaluate the interpolation points, may be null for literals.
     * @return a function interpolating lookup values.
     */
    @Override
    public Function<Object,Object> bind(final R input) {
        /*
         * Evaluate the points, then determine the method if it was not specified.
         * Each point is stored as an array with the data value followed by the components.
         */
        final List<Object> values = new ArrayList<>(interpolationPoints.size());
        final List<Double> data   = new ArrayList<>(interpolationPoints.size());
        for (InterpolationPoint<R> p : interpolationPoints) {
            final double d = number(p.getData().apply(input));
            if (!Double.isNaN(d)) {
                data.add(d);
                values.add(p.getValue().apply(input));
            }
        }
        boolean isColor = (method == Method.COLOR);
        if (method == null) {
            for (Object value : values) {
                if (Double.isNaN(number(value)) && color(value) != null) {
                    isColor = true;
                    break;
                }
            }
        }
        final List<double[]> points = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            final Object value = values.get(i);
            if (isColor) {
                final Color c = color(value);
                if (c != null) points.add(new double[] {data.get(i), c.getAlpha(), c.getRed(), c.getGreen(), c.getBlue()});
            } else {
                final double n = number(value);
                if (!Double.isNaN(n)) points.add(new double[] {data.get(i), n});
            }
        }
        points.sort((a, b) -> Double.compare(a[0], b[0]));
        final double[][] p = points.toArray(double[][]::new);
        final Object fallback = fallback(input);
        final boolean asColor = isColor;
        return (lookup) -> {
            final double value = number(lookup);
            final int n = p.length;
            if (Double.isNaN(value) || n == 0) {
                return fallback;
            }
            final double[] c;
            if (value <= p[0][0]) {
                c = p[0];
            } else if (value >= p[n - 1][0]) {
                c = p[n - 1];
            } else {
                int i = 1;
                while (p[i][0] < value) i++;          // p[i-1] < value <= p[i]
                c = interpolate(p, i, (value - p[i - 1][0]) / (p[i][0] - p[i - 1][0]));
            }
            if (asColor) {
                return new Color(component(c[2]), component(c[3]), component(c[4]), component(c[1]));
            }
            return c[1];
        };
    }

    /**
     * Interpolates all components between point <var>i</var>-1 and point <var>i</var>.
     *
     * @param  p  the points, with the data value at index 0 followed by the components.
     * @param  i  index of the point after the lookup value.
     * @param  t  relative position of the lookup value between the two points, in [0 … 1].
     * @return the data value (not interpolated) followed by the interpolated components.
     */
    private double[] interpolate(final double[][] p, final int i, final double t) {
        final double[] c = Arrays.copyOf(p[i], p[i].length);
        for (int k = 1; k < c.length; k++) {
            final double c1 = p[i - 1][k];
            final double c2 = p[i][k];
            switch (mode) {
                case COSINE: {
                    c[k] = c1 + (c2 - c1) * (1 - Math.cos(Math.PI * t)) / 2;
                    break;
                }
                case CUBIC: {
                    // Catmull-Rom spline, with the end points duplicated.
                    final double c0 = p[Math.max(i - 2, 0)][k];
                    final double c3 = p[Math.min(i + 1, p.length - 1)][k];
                    c[k] = 0.5 * ((2 * c1) + (c2 - c0) * t
                            + (2 * c0 - 5 * c1 + 4 * c2 - c3) * t * t
                            + (3 * (c1 - c2) + c3 - c0) * t * t * t);
                    break;
                }
                default: {
                    c[k] = c1 + (c2 - c1) * t;
                    break;
                }
            }
        }
        return c;
    }

    /**
     * Rounds and clamps a color component to the [0 … 255] range.
     */
    private static int component(final double value) {
        return (int) Math.max(0, Math.min(255, Math.round(value)));
    }

    /**
    * A point of an {@link Interpolate} function, associating a data value to a value such as a color.
    *
    * @author  Johann Sorel (Geomatys)
    * @param <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
    * @see Interpolate#getInterpolationPoints()
    */
   public static final class InterpolationPoint<R> implements Serializable {
       /**
        * For cross-version compatibility.
        */
       private static final long serialVersionUID = 3217735208736401874L;

       /**
        * The data value of this point.
        */
       private final Expression<R, ? extends Number> data;

       /**
        * The value associated to the data.
        */
       private final Expression<R,?> value;

       /**
        * Creates a new interpolation point.
        *
        * @param  data   the data value, usually a literal.
        * @param  value  the value associated to the data, usually a color literal.
        */
       public InterpolationPoint(final Expression<R, ? extends Number> data, final Expression<R,?> value) {
           this.data  = Objects.requireNonNull(data,  "data");
           this.value = Objects.requireNonNull(value, "value");
       }

       /**
        * Returns the data value of this point.
        *
        * @return the data value.
        */
       public Expression<R, ? extends Number> getData() {
           return data;
       }

       /**
        * Returns the value associated to the data.
        *
        * @return the value associated to the data.
        */
       public Expression<R,?> getValue() {
           return value;
       }

       /**
        * Compares this point with the given object for equality.
        *
        * @param  obj  the object to compare with this point.
        * @return whether the two objects are equal.
        */
       @Override
       public boolean equals(final Object obj) {
           if (obj instanceof InterpolationPoint) {
               final var other = (InterpolationPoint<?>) obj;
               return data.equals(other.data) && value.equals(other.value);
           }
           return false;
       }

       /**
        * Returns a hash code value for this point.
        *
        * @return a hash code value.
        */
       @Override
       public int hashCode() {
           return data.hashCode() * 31 + value.hashCode();
       }

       /**
        * Returns a string representation of this point for debugging purposes.
        *
        * @return a string representation of this point.
        */
       @Override
       public String toString() {
           return "(" + data + ", " + value + ')';
       }
   }

}
