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
package org.apache.sis.coverage;

import java.util.Comparator;
import java.util.List;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.measure.Unit;
import org.apache.sis.util.logging.Logging;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.referencing.operation.MathTransform1D;
import org.apache.sis.referencing.operation.transform.TransferFunction;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.Debug;


/**
 * Describes the data values in a coverage (the range). For a raster, a sample dimension is a band.
 * A sample dimension can reserve some values for <cite>qualitative</cite> information like  “this
 * is a forest” and some other values for <cite>quantitative</cite> information like a temperature
 * measurements.
 *
 * <div class="note"><b>Example:</b>
 * an image of sea surface temperature (SST) could define the following categories:
 * <table class="sis">
 *   <caption>Example of categories in a sample dimension</caption>
 *   <tr><th>Values range</th> <th>Meaning</th></tr>
 *   <tr><td>[0]</td>          <td>No data</td></tr>
 *   <tr><td>[1]</td>          <td>Cloud</td></tr>
 *   <tr><td>[2]</td>          <td>Land</td></tr>
 *   <tr><td>[10…210]</td>     <td>Temperature to be converted into Celsius degrees through a linear equation</td></tr>
 * </table>
 * In this example, sample values in range [10…210] define a quantitative category, while all others categories are qualitative.
 * </div>
 *
 * <h2>Relationship with metadata</h2>
 * This class provides the same information than ISO 19115 {@link org.opengis.metadata.content.SampleDimension},
 * but organized in a different way. The use of the same name may seem a risk, but those two types are typically
 * not used at the same time.
 *
 * <h2>Definition of missing data</h2>
 * An important aspect of sample dimensions is the {@link #getBackground() background value}. It defines how to
 * initialize an empty canvas/matrix with respect to the sample definition. It is the value that defines the main/
 * default "lack of data" (fill value, no-data, missing value) category.
 *
 * It is recommended to define it to enforce stable behavior upon associated data processing. As a last resort, sample
 * dimension {@link Builder builder} will try to identify an acceptable default background value by searching for a
 * qualitative category named <q>background</q>, <q>fill-value</q> or <q>no-data</q>.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
 *
 * @see org.opengis.metadata.content.SampleDimension
 *
 * @since 1.0
 * @module
 */
public class SampleDimension implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4966135180995819364L;

    /**
     * Identification for this sample dimension. Typically used as a way to perform a band select by
     * using human comprehensible descriptions instead of just numbers. Web Coverage Service (WCS)
     * can use this name in order to perform band sub-setting as directed from a user request.
     *
     * @see #getName()
     */
    private final GenericName name;

    /**
     * The background value, or {@code null} if unspecified. Should be a sample value of
     * a qualitative category in the {@link #categories} list, but this is not mandatory.
     *
     * @see #getBackground()
     */
    private final Number background;

    /**
     * The list of categories making this sample dimension. May be empty but shall never be null.
     */
    private final CategoryList categories;

    /**
     * The transform from samples to real values. May be {@code null} if this sample dimension
     * does not define any transform (which is not the same than defining an identity transform).
     *
     * @see #getTransferFunction()
     */
    private final MathTransform1D transferFunction;

    /**
     * The {@code SampleDimension} that describes values after {@linkplain #getTransferFunction() transfer function}
     * has been applied, or if this {@code SampleDimension} is already converted then the original sample dimension.
     * May be {@code null} if this sample dimension has no transfer function, or {@code this} if the transfer function
     * is the identity function.
     *
     * <p>This field establishes a bidirectional navigation between sample values and real values.
     * This is in contrast with methods named {@link #converted()}, which establish a unidirectional
     * navigation from sample values to real values.</p>
     *
     * @see #converted()
     * @see Category#converse
     * @see CategoryList#converse
     */
    private final SampleDimension converse;

    /**
     * Creates a new sample dimension for values that are already converted to real values.
     * This transfer function is set to identity, which implies that this constructor should
     * be invoked only for sample dimensions having at least one quantitative category.
     *
     * @param  original  the original sample dimension for packed values.
     * @param  bc        category of the background value in original sample dimension, or {@code null}.
     */
    private SampleDimension(final SampleDimension original, final Category bc) {
        converse         = original;
        name             = original.name;
        categories       = original.categories.converse;
        transferFunction = Category.identity();
        background       = (bc != null) ? bc.converse.range.getMinValue() : null;
    }

    /**
     * Creates a sample dimension with the specified name and categories.
     * The sample dimension name is used as a way to perform a band select
     * by using human comprehensible descriptions instead of numbers.
     * The background value is used for filling empty space in map reprojections.
     * The background value (if specified) should be the value of a qualitative category
     * present in the {@code categories} collection, but this is not mandatory.
     *
     * <p>Note that {@link Builder} provides a more convenient way to create sample dimensions.</p>
     *
     * @param name        an identification for the sample dimension.
     * @param background  the background value, or {@code null} if none.
     * @param categories  the list of categories. May be empty if none.
     * @throws IllegalSampleDimensionException if two or more categories have overlapping sample value range.
     */
    public SampleDimension(final GenericName name, final Number background, final Collection<? extends Category> categories) {
        ArgumentChecks.ensureNonNull("name", name);
        ArgumentChecks.ensureNonNull("categories", categories);
        final CategoryList list;
        if (categories.isEmpty()) {
            list = CategoryList.EMPTY;
        } else {
            list = CategoryList.create(categories.toArray(new Category[categories.size()]), background);
        }
        this.name       = name;
        this.background = background;
        this.categories = list;
        if (list.converse.range == null) {                  // Case where there is no quantitative category.
            transferFunction = null;
            converse = null;
        } else if (list == list.converse) {                 // Case where values are already converted.
            transferFunction = Category.identity();
            converse = this;
        } else {
            assert !list.isEmpty();                         // If empty, list.converse.range would have been null.
            transferFunction = list.getTransferFunction();
            converse = new SampleDimension(this, (background != null) ? list.search(background.doubleValue()) : null);
        }
    }

    /**
     * Returns the sample dimension that describes real values. This method establishes a unidirectional navigation
     * from sample values to real values. This is in contrast to {@link #converse}, which establish a bidirectional
     * navigation.
     *
     * @see #forConvertedValues(boolean)
     */
    private SampleDimension converted() {
        // Transfer function shall never be null if 'converse' is non-null.
        return (converse != null && !transferFunction.isIdentity()) ? converse : this;
    }

    /**
     * Returns an identification for this sample dimension. This is typically used as a way to perform a band select
     * by using human comprehensible descriptions instead of just numbers. Web Coverage Service (WCS) can use this name
     * in order to perform band sub-setting as directed from a user request.
     *
     * @return an identification of this sample dimension.
     *
     * @see org.opengis.metadata.content.RangeDimension#getSequenceIdentifier()
     */
    public GenericName getName() {
        return name;
    }

    /**
     * Returns all categories in this sample dimension. Note that a {@link Category} object may apply to an arbitrary range
     * of sample values. Consequently, the first element in this collection may not be directly related to the sample value
     * {@code 0}.
     *
     * @return the list of categories in this sample dimension, or an empty list if none.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<Category> getCategories() {
        return categories;                      // Safe to return because immutable.
    }

    /**
     * Returns the background value. If this sample dimensions has quantitative categories, then the background
     * value should be one of the value returned by {@link #getNoDataValues()}. However this is not mandatory.
     *
     * @return the background value, typically (but not necessarily) one of {@link #getNoDataValues()}.
     */
    public Optional<Number> getBackground() {
        return Optional.ofNullable(background);
    }

    /**
     * Returns the values to indicate "no data" for this sample dimension.
     * If the sample dimension describes {@linkplain #forConvertedValues(boolean) converted values},
     * then the "no data values" are NaN values.
     *
     * @return the values to indicate no data values for this sample dimension, or an empty set if none.
     * @throws IllegalStateException if this method can not expand the range of no data values, for example
     *         because some ranges contain an infinite amount of values.
     *
     * @see #allowsNaN()
     */
    public Set<Number> getNoDataValues() {
        if (converse != null) {             // Null if SampleDimension does not contain at least one quantitative category.
            final boolean isConverted = transferFunction.isIdentity();
            final NumberRange<?>[] ranges = new NumberRange<?>[categories.size()];
            Class<? extends Number> widestClass = Byte.class;
            int count = 0;
            for (final Category category : categories) {
                final Category converted = category.converted();
                if ((isConverted || category != converted) && converted.isConvertedQualitative()) {
                    final NumberRange<?> range = category.range;
                    if (!range.isBounded()) {
                        throw new IllegalStateException(Resources.format(Resources.Keys.CanNotEnumerateValuesInRange_1, range));
                    }
                    widestClass = Numbers.widestClass(widestClass, range.getElementType());
                    ranges[count++] = range;
                }
            }
            if (count != 0) {
                final Set<Number> noDataValues = new TreeSet<>(SampleDimension::compare);
                for (int i=0; i<count; i++) {
                    final NumberRange<?> range = ranges[i];
                    final Number minimum = range.getMinValue();
                    final Number maximum = range.getMaxValue();
                    if (range.isMinIncluded()) noDataValues.add(Numbers.cast(minimum, widestClass));
                    if (range.isMaxIncluded()) noDataValues.add(Numbers.cast(maximum, widestClass));
                    if (Numbers.isInteger(range.getElementType())) {
                        long value = minimum.longValue() + 1;       // If value was inclusive, then it has already been added to the set.
                        long stop  = maximum.longValue() - 1;
                        while (value <= stop) {
                            noDataValues.add(Numbers.wrap(value, widestClass));
                        }
                    } else if (!minimum.equals(maximum)) {
                        throw new IllegalStateException(Resources.format(Resources.Keys.CanNotEnumerateValuesInRange_1, range));
                    }
                }
                return noDataValues;
            }
        }
        return Collections.emptySet();
    }

    /**
     * Compares as {@code double} values. This method is similar to {@link Double#compare(double,double)}
     * except that it also orders NaN values from raw bit patterns. Reminder: NaN values are sorted last.
     */
    private static int compare(final Number n1, final Number n2) {
        return Category.compare(n1.doubleValue(), n2.doubleValue());
    }

    /**
     * Returns the range of values occurring in this sample dimension. The range delimits sample values that
     * can be converted into real values using the {@linkplain #getTransferFunction() transfer function}.
     * If that function is {@linkplain MathTransform1D#isIdentity() identity}, then the values are already
     * real values and the range may be an instance of {@link MeasurementRange}
     * (i.e. a number range with units of measurement).
     *
     * @return the range of sample values in this sample dimension.
     */
    public Optional<NumberRange<?>> getSampleRange() {
        return Optional.ofNullable(categories.range);
    }

    /**
     * Returns the range of values after conversions by the transfer function.
     * This range is absent if there is no transfer function.
     *
     * @return the range of values after conversion by the transfer function.
     *
     * @see #getUnits()
     */
    public Optional<MeasurementRange<?>> getMeasurementRange() {
        if (converse == null) {
            return Optional.empty();
        }
        // A ClassCastException below would be a bug in our constructors.
        return Optional.ofNullable((MeasurementRange<?>) converted().categories.range);
    }

    /**
     * Returns the <cite>transfer function</cite> from sample values to real values.
     * This method returns a transform expecting sample values as input and computing real values as output.
     * The output units of measurement is given by {@link #getUnits()}.
     *
     * <p>This transform takes care of converting all "{@linkplain #getNoDataValues() no data values}" into {@code NaN} values.
     * The <code>transferFunction.{@linkplain MathTransform1D#inverse() inverse()}</code> transform is capable to differentiate
     * those {@code NaN} values and get back the original sample value.</p>
     *
     * @return the <cite>transfer function</cite> from sample to real values. May be absent if this sample dimension
     *         does not define any transform (which is not the same that defining an identity transform).
     */
    public Optional<MathTransform1D> getTransferFunction() {
        return Optional.ofNullable(transferFunction);
    }

    /**
     * Returns the scale factor and offset of the transfer function.
     * The formula returned by this method does <strong>not</strong> take
     * "{@linkplain #getNoDataValues() no data values}" in account.
     * For a more generic transfer function, see {@link #getTransferFunction()}.
     *
     * @return a description of the part of the transfer function working on real numbers.
     * @throws IllegalStateException if the transfer function can not be simplified in a form representable
     *         by {@link TransferFunction}.
     */
    public Optional<TransferFunction> getTransferFunctionFormula() {
        MathTransform1D tr = null;
        for (final Category category : categories) {
            final Optional<MathTransform1D> c = category.getTransferFunction();
            if (c.isPresent()) {
                if (tr == null) {
                    tr = c.get();
                } else if (!tr.equals(c.get())) {
                    throw new IllegalStateException(Resources.format(Resources.Keys.CanNotSimplifyTransferFunction_1));
                }
            }
        }
        if (tr == null) {
            return Optional.empty();
        }
        final TransferFunction f = new TransferFunction();
        try {
            f.setTransform(tr);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(Resources.format(Resources.Keys.CanNotSimplifyTransferFunction_1, e));
        }
        return Optional.of(f);
    }

    /**
     * Returns the units of measurement for this sample dimension.
     * This unit applies to values obtained after the {@linkplain #getTransferFunction() transfer function}.
     * May be absent if not applicable.
     *
     * @return the units of measurement.
     * @throws IllegalStateException if this sample dimension use different units.
     *
     * @see #getMeasurementRange()
     */
    public Optional<Unit<?>> getUnits() {
        Unit<?> unit = null;
        for (final Category category : converted().categories) {
            final NumberRange<?> r = category.range;
            if (r instanceof MeasurementRange<?>) {
                final Unit<?> c = ((MeasurementRange<?>) r).unit();
                if (c != null) {
                    if (unit == null) {
                        unit = c;
                    } else if (!unit.equals(c)) {
                        throw new IllegalStateException();
                    }
                }
            }
        }
        return Optional.ofNullable(unit);
    }

    /**
     * Returns {@code true} if some sample values can be {@link Float#NaN NaN} values.
     * It may be the case for {@linkplain #forConvertedValues(boolean) converted values},
     * but not necessarily (because a coverage does not necessarily allow missing values).
     * If {@code true}, then the NaN values should be listed by {@link #getNoDataValues()}.
     *
     * @return whether some values in this sample dimension can be {@link Float#NaN NaN}.
     *
     * @see #getNoDataValues()
     * @see org.apache.sis.math.MathFunctions#toNanFloat(int)
     *
     * @since 1.1
     */
    public boolean allowsNaN() {
        return categories.allowsNaN();
    }

    /**
     * Returns a sample dimension that describes real values or sample values, depending if {@code converted} is {@code true}
     * or {@code false} respectively.  If there is no {@linkplain #getTransferFunction() transfer function}, then this method
     * returns {@code this}.
     *
     * @param  converted  {@code true} for a sample dimension describing converted values,
     *                    or {@code false} for a sample dimension describing packed values.
     * @return a sample dimension describing converted or packed values, depending on {@code converted} argument value.
     *         May be {@code this} but never {@code null}.
     *
     * @see Category#forConvertedValues(boolean)
     * @see org.apache.sis.coverage.grid.GridCoverage#forConvertedValues(boolean)
     */
    public SampleDimension forConvertedValues(final boolean converted) {
        // Transfer function shall never be null if 'converse' is non-null.
        if (converse != null && transferFunction.isIdentity() != converted) {
            return converse;
        }
        return this;
    }

    /**
     * Returns a hash value for this sample dimension.
     */
    @Override
    public int hashCode() {
        return categories.hashCode() + 31*name.hashCode();
    }

    /**
     * Compares the specified object with this sample dimension for equality.
     *
     * @param  object  the object to compare with.
     * @return {@code true} if the given object is equals to this sample dimension.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof SampleDimension) {
            final SampleDimension that = (SampleDimension) object;
            return name.equals(that.name) && Objects.equals(background, that.background) && categories.equals(that.categories);
        }
        return false;
    }

    /**
     * Returns a string representation of this sample dimension.
     * This string is for debugging purpose only and may change in future version.
     *
     * @return a string representation of this sample dimension for debugging purpose.
     */
    @Override
    public String toString() {
        return new SampleRangeFormat(Locale.getDefault()).write(new SampleDimension[] {this});
    }

    /**
     * Returns a string representation of the given sample dimensions.
     * This string is for debugging purpose only and may change in future version.
     *
     * @param  locale      the locale to use for formatting texts.
     * @param  dimensions  the sample dimensions to format.
     * @return a string representation of the given sample dimensions for debugging purpose.
     */
    @Debug
    public static String toString(final Locale locale, SampleDimension... dimensions) {
        ArgumentChecks.ensureNonNull("dimensions", dimensions);
        return new SampleRangeFormat(locale).write(dimensions);
    }




    /**
     * A mutable builder for creating an immutable {@link SampleDimension}.
     * The following properties can be set:
     *
     * <ul>
     *   <li>An optional name for the {@code SampleDimension}.</li>
     *   <li>A single optional category for the background value.</li>
     *   <li>An arbitrary amount of <cite>qualitative</cite> categories.</li>
     *   <li>An arbitrary amount of <cite>quantitative</cite> categories.</li>
     * </ul>
     *
     * <p>A <cite>qualitative category</cite> is a range of sample values associated to a label.
     * For example 0 = no data, 1 = cloud, 2 = sea, 3 = land, <i>etc</i>.
     * Missing values are also considered as a qualitative category and should be declared.
     * If the missing value can be used as a background value for filling empty spaces in
     * {@linkplain org.apache.sis.image.ImageProcessor#resample image resampling operations},
     * then it should be declared using {@code setBackground(…)} method instead of {@code addQualitative(…)}.</p>
     *
     * <p>A <cite>quantitative category</cite> is a range of sample values associated to numbers with units of measurement.
     * For example 10 = 1.0°C, 11 = 1.1°C, 12 = 1.2°C, <i>etc</i>. A quantitative category has a
     * {@linkplain org.opengis.metadata.content.SampleDimension#getTransferFunctionType() transfer function}
     * (typically a scale factor and an offset) for converting sample values to values expressed
     * in the unit of measurement.</p>
     *
     * <p>Qualitative and quantitative categories can be mixed in the same {@link SampleDimension},
     * provided that their ranges do not overlap.
     * After properties have been set, the sample dimension is created by invoking {@link #build()}.</p>
     *
     * <h2>Note for subclass implementations</h2>
     * Properties are ultimately set by the following methods:
     *
     * <ul>
     *   <li>{@link #setName(GenericName)}</li>
     *   <li>{@link #setBackground(CharSequence, Number)}</li>
     *   <li>{@link #addQualitative(CharSequence, NumberRange)}</li>
     *   <li>{@link #addQuantitative(CharSequence, NumberRange, MathTransform1D, Unit)}</li>
     * </ul>
     *
     * All other {@code setName(…)}, {@code addQualitative(…)} and {@code addQuantitative(…)} methods
     * are convenience methods delegating to above-cited methods, thus providing single points that
     * subclasses can override.
     *
     * @author  Martin Desruisseaux (IRD, Geomatys)
     * @version 1.2
     * @since   1.0
     * @module
     */
    public static class Builder {
        /**
         * Identification for this sample dimension.
         */
        private GenericName dimensionName;

        /**
         * The categories for the sample dimension to create.
         * This list is modified by the following methods:
         *
         * <ul>
         *   <li>{@link #setBackground(CharSequence, Number)}</li>
         *   <li>{@link #addQualitative(CharSequence, NumberRange)}</li>
         *   <li>{@link #addQuantitative(CharSequence, NumberRange, MathTransform1D, Unit)}</li>
         *   <li>{@code categories().remove(int)}</li>
         *   <li>{@link #clear()}</li>
         * </ul>
         *
         * @see #categories()
         */
        private Category[] categories;

        /**
         * Number of valid elements in {@link #categories}.
         */
        private int count;

        /**
         * The ordinal NaN values used for this sample dimension.
         * The {@link Category} constructor uses this set for avoiding collisions.
         */
        private final ToNaN toNaN;

        /**
         * Creates an initially empty builder for a sample dimension.
         * Callers shall invoke at least one {@code addFoo(…)} method before {@link #build()}.
         */
        public Builder() {
            categories = new Category[10];
            toNaN      = new ToNaN();
        }

        /**
         * Sets an identification of the sample dimension.
         * This is the value to be returned by {@link SampleDimension#getName()}.
         * If this method is invoked more than once, then the last specified name prevails
         * (previous sample dimension names are discarded).
         *
         * @param  name  identification of the sample dimension.
         * @return {@code this}, for method call chaining.
         */
        public Builder setName(final GenericName name) {
            dimensionName = name;
            return this;
        }

        /**
         * Sets an identification of the sample dimension as a character sequence.
         * This is a convenience method for creating a {@link GenericName} from the given characters.
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #setName(GenericName)}.</div>
         *
         * @param  name  identification of the sample dimension.
         * @return {@code this}, for method call chaining.
         */
        public Builder setName(final CharSequence name) {
            return setName(createLocalName(name));
        }

        /**
         * Sets an identification of the sample dimension as a band number.
         * This method should be used only when no more descriptive name is available.
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #setName(GenericName)}.</div>
         *
         * @param  band  sequence identifier of the sample dimension to create.
         * @return {@code this}, for method call chaining.
         */
        public Builder setName(final int band) {
            return setName(Names.createMemberName(null, null, band));
        }

        /**
         * A common place where are created local names from character string.
         * For making easier to revisit if we want to add a namespace.
         */
        private static GenericName createLocalName(final CharSequence name) {
            return Names.createLocalName(null, null, name);
        }

        /**
         * Creates a range for the given minimum and maximum values. We use the static factory methods instead
         * than the {@link NumberRange} constructor for sharing existing range instances. This is also a way
         * to ensure that the number type is one of the primitive wrappers.
         *
         * <p>This method is invoked for qualitative categories only. For that reason, it accepts NaN values.</p>
         */
        private static NumberRange<?> range(final Class<?> type, Number minimum, Number maximum) {
            switch (Numbers.getEnumConstant(type)) {
                case Numbers.BYTE:    return NumberRange.create(minimum.byteValue(),  true, maximum.byteValue(),   true);
                case Numbers.SHORT:   return NumberRange.create(minimum.shortValue(), true, maximum.shortValue(),  true);
                case Numbers.INTEGER: return NumberRange.create(minimum.intValue(),   true, maximum.intValue(),    true);
                case Numbers.LONG:    return NumberRange.create(minimum.longValue(),  true, maximum.longValue(),   true);
                case Numbers.FLOAT: {
                    final float min = minimum.floatValue();
                    final float max = maximum.floatValue();
                    if (!Float.isNaN(min) || !Float.isNaN(max)) {       // Let 'create' throws an exception if only one value is NaN.
                        return NumberRange.create(min, true, max, true);
                    }
                    if (minimum.getClass() != Float.class) minimum = min;
                    if (maximum.getClass() != Float.class) maximum = max;
                    break;
                }
                default: {
                    final double min = minimum.doubleValue();
                    final double max = maximum.doubleValue();
                    if (!Double.isNaN(min) || !Double.isNaN(max)) {     // Let 'create' throws an exception if only one value is NaN.
                        return NumberRange.create(min, true, max, true);
                    }
                    if (minimum.getClass() != Double.class) minimum = min;
                    if (maximum.getClass() != Double.class) maximum = max;
                    break;
                }
            }
            @SuppressWarnings({"unchecked", "rawtypes"})
            final NumberRange<?> samples = new NumberRange(type, minimum, true, maximum, true);
            return samples;
        }

        /**
         * Adds a qualitative category and marks that category as the background value.
         * This is the value to be returned by {@link SampleDimension#getBackground()}.
         * If this method is invoked more than once, then the last specified value prevails
         * (previous values become ordinary qualitative categories).
         *
         * @param  name    the category name as a {@link String} or {@link InternationalString} object,
         *                 or {@code null} for a default "fill value" name.
         * @param  sample  the background value.
         * @return {@code this}, for method call chaining.
         */
        public Builder setBackground(CharSequence name, Number sample) {
            ArgumentChecks.ensureNonNull("sample", sample);
            if (name == null) {
                name = Vocabulary.formatInternational(Vocabulary.Keys.FillValue);
            }
            final NumberRange<?> samples = range(sample.getClass(), sample, sample);
            // Use of 'getMinValue()' below shall be consistent with ToNaN.remove(Category).
            toNaN.background = samples.getMinValue();
            add(new Category(name, samples, null, null, toNaN));
            return this;
        }

        /**
         * Adds a qualitative category for samples of the given boolean value.
         * The {@code true} value is represented by 1 and the {@code false} value is represented by 0.
         * <p>
         * It is strongly recommended using {@link #setBackground(CharSequence, Number)} instead of this method when the
         * aim is to define a main/primary fill value.
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #addQualitative(CharSequence, NumberRange)}.</div>
         *
         * @param  name    the category name as a {@link String} or {@link InternationalString} object,
         *                 or {@code null} for a default "no data" name.
         * @param  sample  the sample value as a boolean.
         * @return {@code this}, for method call chaining.
         */
        public Builder addQualitative(final CharSequence name, final boolean sample) {
            final byte value = sample ? (byte) 1 : 0;
            return addQualitative(name, NumberRange.create(value, true, value, true));
        }

        /**
         * Adds a qualitative category for samples of the given tiny (8 bits) integer value.
         * The argument is treated as a signed integer ({@value Byte#MIN_VALUE} to {@value Byte#MAX_VALUE}).
         * <p>
         * It is strongly recommended using {@link #setBackground(CharSequence, Number)} instead of this method when the
         * aim is to define a main/primary fill value.
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #addQualitative(CharSequence, NumberRange)}.</div>
         *
         * @param  name    the category name as a {@link String} or {@link InternationalString} object,
         *                 or {@code null} for a default "no data" name.
         * @param  sample  the sample value as an integer.
         * @return {@code this}, for method call chaining.
         */
        public Builder addQualitative(final CharSequence name, final byte sample) {
            return addQualitative(name, NumberRange.create(sample, true, sample, true));
        }

        /**
         * Adds a qualitative category for samples of the given short (16 bits) integer value.
         * The argument is treated as a signed integer ({@value Short#MIN_VALUE} to {@value Short#MAX_VALUE}).
         * <p>
         * It is strongly recommended using {@link #setBackground(CharSequence, Number)} instead of this method when the
         * aim is to define a main/primary fill value.
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #addQualitative(CharSequence, NumberRange)}.</div>
         *
         * @param  name    the category name as a {@link String} or {@link InternationalString} object,
         *                 or {@code null} for a default "no data" name.
         * @param  sample  the sample value as an integer.
         * @return {@code this}, for method call chaining.
         */
        public Builder addQualitative(final CharSequence name, final short sample) {
            return addQualitative(name, NumberRange.create(sample, true, sample, true));
        }

        /**
         * Adds a qualitative category for samples of the given integer value.
         * The argument is treated as a signed integer ({@value Integer#MIN_VALUE} to {@value Integer#MAX_VALUE}).
         * <p>
         * It is strongly recommended using {@link #setBackground(CharSequence, Number)} instead of this method when the
         * aim is to define a main/primary fill value.
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #addQualitative(CharSequence, NumberRange)}.</div>
         *
         * @param  name    the category name as a {@link String} or {@link InternationalString} object,
         *                 or {@code null} for a default "no data" name.
         * @param  sample  the sample value as an integer.
         * @return {@code this}, for method call chaining.
         */
        public Builder addQualitative(final CharSequence name, final int sample) {
            return addQualitative(name, NumberRange.create(sample, true, sample, true));
        }

        /**
         * Adds a qualitative category for samples of the given floating-point value.
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #addQualitative(CharSequence, NumberRange)}.</div>
         * <p>
         * It is strongly recommended using {@link #setBackground(CharSequence, Number)} instead of this method when the
         * aim is to define a main/primary fill value.
         *
         * @param  name    the category name as a {@link String} or {@link InternationalString} object,
         *                 or {@code null} for a default "no data" name.
         * @param  sample  the sample value as a real number or a NaN value.
         * @return {@code this}, for method call chaining.
         */
        public Builder addQualitative(final CharSequence name, final float sample) {
            final NumberRange<Float> range;
            if (Float.isNaN(sample)) {
                final Float wrapper = sample;
                range = new NumberRange<>(Float.class, wrapper, true, wrapper, true);
            } else {
                range = NumberRange.create(sample, true, sample, true);
            }
            return addQualitative(name, range);
        }

        /**
         * Adds a qualitative category for samples of the given double precision floating-point value.
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #addQualitative(CharSequence, NumberRange)}.</div>
         * <p>
         * It is strongly recommended using {@link #setBackground(CharSequence, Number)} instead of this method when the
         * aim is to define a main/primary fill value.
         *
         * @param  name    the category name as a {@link String} or {@link InternationalString} object,
         *                 or {@code null} for a default "no data" name.
         * @param  sample  the sample value as a real number or a NaN value.
         * @return {@code this}, for method call chaining.
         */
        public Builder addQualitative(final CharSequence name, final double sample) {
            final NumberRange<Double> range;
            if (Double.isNaN(sample)) {
                final Double wrapper = sample;
                range = new NumberRange<>(Double.class, wrapper, true, wrapper, true);
            } else {
                range = NumberRange.create(sample, true, sample, true);
            }
            return addQualitative(name, range);
        }

        /**
         * Adds a qualitative category for samples in the given range of values.
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #addQualitative(CharSequence, NumberRange)}.</div>
         * <p>
         * It is strongly recommended using {@link #setBackground(CharSequence, Number)} instead of this method when the
         * aim is to define a main/primary fill value.
         *
         * @param  name     the category name as a {@link String} or {@link InternationalString} object,
         *                  or {@code null} for a default "no data" name.
         * @param  minimum  the minimum sample value, inclusive.
         * @param  maximum  the maximum sample value, inclusive.
         * @return {@code this}, for method call chaining.
         * @throws IllegalArgumentException if the range is empty.
         */
        public Builder addQualitative(final CharSequence name, final Number minimum, final Number maximum) {
            ArgumentChecks.ensureNonNull("minimum", minimum);
            ArgumentChecks.ensureNonNull("maximum", maximum);
            return addQualitative(name, range(Numbers.widestClass(minimum, maximum), minimum, maximum));
        }

        /**
         * Adds a qualitative category for all samples in the specified range of values.
         * This is the most generic method for adding a qualitative category.
         * All other {@code addQualitative(name, …)} methods are convenience methods delegating their work to this method.
         * <p>
         * It is strongly recommended using {@link #setBackground(CharSequence, Number)} instead of this method when the
         * aim is to define a main/primary fill value.
         *
         * @param  name     the category name as a {@link String} or {@link InternationalString} object,
         *                  or {@code null} for a default "no data" name.
         * @param  samples  the minimum and maximum sample values in the category.
         * @return {@code this}, for method call chaining.
         * @throws IllegalArgumentException if the given range is empty.
         */
        public Builder addQualitative(CharSequence name, final NumberRange<?> samples) {
            if (name == null) {
                name = Vocabulary.formatInternational(Vocabulary.Keys.Nodata);
            }
            add(new Category(name, samples, null, null, toNaN));
            return this;
        }

        /**
         * Adds a qualitative category for the given sample value mapped to the specified converted NaN value.
         * The given {@code converted} value must be {@linkplain Float#isNaN(float) a NaN value}
         * (not necessarily the {@link Float#NaN} canonical value)
         * and that value shall not have been used by another category.
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #mapQualitative(CharSequence, NumberRange, float)}.</div>
         *
         * @param  name       the category name as a {@link String} or {@link InternationalString} object,
         *                    or {@code null} for a default "no data" name.
         * @param  sample     the sample value as a real or integer number.
         * @param  converted  the converted value to map to the given sample value.
         *                    {@code Float.isNaN(converted)} must be {@code true}.
         * @return {@code this}, for method call chaining.
         * @throws IllegalArgumentException if {@code converted} is not a NaN value.
         *
         * @see MathFunctions#toNanFloat(int)
         *
         * @since 1.1
         */
        public Builder mapQualitative(CharSequence name, final Number sample, final float converted) {
            ArgumentChecks.ensureNonNull("sample", sample);
            return mapQualitative(name, range(sample.getClass(), sample, sample), converted);
        }

        /**
         * Adds a qualitative category for the given samples values mapped to the specified converted NaN value.
         * The given {@code converted} value must be {@linkplain Float#isNaN(float) a NaN value}
         * (not necessarily the {@link Float#NaN} canonical value)
         * and that value shall not have been used by another category.
         *
         * <p>The {@code addQualitative(…)} methods select automatically a converted value.
         * The {@code mapQualitative(…)} methods are for the less common case where caller
         * needs to control which converted NaN value is mapped to the samples values.</p>
         *
         * @param  name       the category name as a {@link String} or {@link InternationalString} object,
         *                    or {@code null} for a default "no data" name.
         * @param  samples    the minimum and maximum sample values in the category.
         * @param  converted  the converted value to map to the given sample values.
         *                    {@code Float.isNaN(converted)} must be {@code true}.
         * @return {@code this}, for method call chaining.
         * @throws IllegalArgumentException if {@code converted} is not a NaN value.
         *
         * @see MathFunctions#toNanFloat(int)
         *
         * @since 1.1
         */
        public Builder mapQualitative(CharSequence name, final NumberRange<?> samples, final float converted) {
            ArgumentChecks.ensureNonNull("samples", samples);
            final int ordinal = MathFunctions.toNanOrdinal(converted);
            if (!toNaN.add(ordinal)) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.ValueAlreadyDefined_1, "NaN #" + ordinal));
            }
            if (name == null) {
                name = Vocabulary.formatInternational(Vocabulary.Keys.Nodata);
            }
            add(new Category(name, samples, null, null, (v) -> ordinal));
            return this;
        }

        /**
         * Constructs a quantitative category mapping samples to real values in the specified range.
         * Sample values in the {@code samples} range will be mapped to real values in the {@code converted} range
         * through a linear equation of the form:
         *
         * <blockquote><var>measure</var> = <var>sample</var> × <var>scale</var> + <var>offset</var></blockquote>
         *
         * where <var>scale</var> and <var>offset</var> coefficients are computed from the ranges supplied in arguments.
         * The units of measurement will be taken from the {@code converted} range if it is an instance of {@link MeasurementRange}.
         *
         * <p><b>Warning:</b> this method is provided for convenience when the scale and offset factors are not explicitly specified.
         * If those factor are available, then the other {@code addQuantitative(name, samples, …)} methods are more reliable.</p>
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #addQuantitative(CharSequence, NumberRange, MathTransform1D, Unit)}.</div>
         *
         * @param  name        the category name as a {@link String} or {@link InternationalString} object.
         * @param  samples     the minimum and maximum sample values in the category. Element class is usually
         *                     {@link Integer}, but {@link Float} and {@link Double} values are accepted as well.
         * @param  converted   the range of real values for this category, as an instance of {@link MeasurementRange}
         *                     if those values are associated to an unit of measurement.
         * @return {@code this}, for method call chaining.
         * @throws ClassCastException if the range element class is not a {@link Number} subclass.
         * @throws IllegalArgumentException if the range is invalid.
         */
        public Builder addQuantitative(final CharSequence name, final NumberRange<?> samples, final NumberRange<?> converted) {
            ArgumentChecks.ensureNonNull("samples", samples);
            ArgumentChecks.ensureNonNull("converted", converted);
            /*
             * We need to perform calculation using the same "included versus excluded" characteristics for sample and converted
             * values. We pickup the characteristics of the range using floating point values because it is easier to adjust the
             * bounds of the range using integer values (we just add or subtract 1 for integers, while the amount to add to real
             * numbers is not so clear). If both ranges use floating point values, arbitrarily adjust the converted values.
             */
            final boolean isMinIncluded, isMaxIncluded;
            if (Numbers.isInteger(samples.getElementType())) {
                isMinIncluded = converted.isMinIncluded();                         // This is the usual case.
                isMaxIncluded = converted.isMaxIncluded();
            } else {
                isMinIncluded = samples.isMinIncluded();                            // Less common case.
                isMaxIncluded = samples.isMaxIncluded();
            }
            final double minValue  = converted.getMinDouble(isMinIncluded);
            final double Δvalue    = converted.getMaxDouble(isMaxIncluded) - minValue;
            final double minSample =   samples.getMinDouble(isMinIncluded);
            final double Δsample   =   samples.getMaxDouble(isMaxIncluded) - minSample;
            final double scale     = Δvalue / Δsample;
            final TransferFunction transferFunction = new TransferFunction();
            transferFunction.setScale(scale);
            transferFunction.setOffset(minValue - scale * minSample);               // TODO: use Math.fma with JDK9.
            return addQuantitative(name, samples, transferFunction.getTransform(),
                    (converted instanceof MeasurementRange<?>) ? ((MeasurementRange<?>) converted).unit() : null);
        }

        /**
         * Adds a quantitative category for values ranging from {@code minimum} to {@code maximum} inclusive
         * in the given units of measurement. The transfer function is set to identity.
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #addQuantitative(CharSequence, NumberRange, MathTransform1D, Unit)}.</div>
         *
         * @param  name     the category name as a {@link String} or {@link InternationalString} object.
         * @param  minimum  the minimum value (inclusive) in the given units.
         * @param  maximum  the maximum value (inclusive) in the given units.
         * @param  units    the units of measurement.
         * @return {@code this}, for method call chaining.
         * @throws IllegalArgumentException if a value is NaN or if {@code minimum} is greater than {@code maximum}.
         */
        public Builder addQuantitative(CharSequence name, float minimum, float maximum, Unit<?> units) {
            return addQuantitative(name, MeasurementRange.create(minimum, true, maximum, true, units), Category.identity(), units);
        }

        /**
         * Adds a quantitative category for values ranging from {@code minimum} to {@code maximum} inclusive
         * in the given units of measurement. The transfer function is set to identity.
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #addQuantitative(CharSequence, NumberRange, MathTransform1D, Unit)}.</div>
         *
         * @param  name     the category name as a {@link String} or {@link InternationalString} object.
         * @param  minimum  the minimum value (inclusive) in the given units.
         * @param  maximum  the maximum value (inclusive) in the given units.
         * @param  units    the units of measurement.
         * @return {@code this}, for method call chaining.
         * @throws IllegalArgumentException if a value is NaN or if {@code minimum} is greater than {@code maximum}.
         */
        public Builder addQuantitative(CharSequence name, double minimum, double maximum, Unit<?> units) {
            return addQuantitative(name, MeasurementRange.create(minimum, true, maximum, true, units), Category.identity(), units);
        }

        /**
         * Adds a quantitative category for sample values ranging from {@code lower} inclusive to {@code upper} exclusive.
         * Sample values are converted into real values using the following linear equation:
         *
         * <blockquote><var>measure</var> = <var>sample</var> × <var>scale</var> + <var>offset</var></blockquote>
         *
         * Results of above conversion are measurements in the units specified by the {@code units} argument.
         *
         * <div class="note"><b>Implementation note:</b>
         * this convenience method delegates to {@link #addQuantitative(CharSequence, NumberRange, MathTransform1D, Unit)}.</div>
         *
         * @param  name    the category name as a {@link String} or {@link InternationalString} object.
         * @param  lower   the lower sample value, inclusive.
         * @param  upper   the upper sample value, exclusive.
         * @param  scale   the scale value which is multiplied to sample values for the category. Must be different than zero.
         * @param  offset  the offset value to add to sample values for this category.
         * @param  units   the units of measurement of values after conversion by the scale factor and offset.
         * @return {@code this}, for method call chaining.
         * @throws IllegalArgumentException if {@code lower} is not smaller than {@code upper},
         *         or if {@code scale} or {@code offset} are not real numbers, or if {@code scale} is zero.
         */
        public Builder addQuantitative(CharSequence name, int lower, int upper, double scale, double offset, Unit<?> units) {
            final TransferFunction transferFunction = new TransferFunction();
            transferFunction.setScale(scale);
            transferFunction.setOffset(offset);
            return addQuantitative(name, NumberRange.create(lower, true, upper, false), transferFunction.getTransform(), units);
        }

        /**
         * Constructs a quantitative category for all samples in the specified range of values.
         * Sample values (usually integers) will be converted into real values
         * (usually floating-point numbers) through the {@code toUnits} transform.
         * Results of that conversion are measurements in the units specified by the {@code units} argument.
         *
         * <p>This is the most generic method for adding a quantitative category.
         * All other {@code addQuantitative(name, …)} methods are convenience methods delegating their work to this method.</p>
         *
         * @param  name     the category name as a {@link String} or {@link InternationalString} object.
         * @param  samples  the minimum and maximum sample values in the category. Element class is usually
         *                  {@link Integer}, but {@link Float} and {@link Double} types are accepted as well.
         * @param  toUnits  the transfer function from sample values to real values in the specified units.
         * @param  units    the units of measurement of values after conversion by the transfer function.
         * @return {@code this}, for method call chaining.
         * @throws ClassCastException if the range element class is not a {@link Number} subclass.
         * @throws IllegalArgumentException if the range is invalid.
         *
         * @see TransferFunction
         */
        public Builder addQuantitative(CharSequence name, NumberRange<?> samples, MathTransform1D toUnits, Unit<?> units) {
            ArgumentChecks.ensureNonNull("toUnits", toUnits);
            add(new Category(name, samples, toUnits, units, toNaN));
            return this;
        }

        /**
         * Adds the given category to the list. This method is not public because the category
         * should have been created with {@link #toNaN} passed in argument to its constructor.
         */
        private void add(final Category category) {
            if (count == categories.length) {
                categories = Arrays.copyOf(categories, count * 2);
            }
            categories[count++] = category;
        }

        /**
         * Returns the list of categories added so far. The returned list does not support the
         * {@link List#add(Object) add} operation, but supports the {@link List#remove(int) remove} operation.
         *
         * @return the current category list, read-only except for the {@code remove} operation.
         */
        public List<Category> categories() {
            return new AbstractList<Category>() {
                /** Returns the number of categories in this list. */
                @Override public int size() {
                    return count;
                }

                /** Returns the category at the given index. */
                @Override public Category get(int i) {
                    ArgumentChecks.ensureValidIndex(count, i);
                    return categories[i];
                }

                /** Removes the category at the given index. */
                @Override public Category remove(int i) {
                    ArgumentChecks.ensureValidIndex(count, i);
                    final Category c = categories[i];
                    System.arraycopy(categories, i+1, categories, i, --count - i);
                    categories[count] = null;
                    toNaN.remove(c);
                    return c;
                }
            };
        }

        /**
         * Creates a new sample with the properties defined to this builder.
         *
         * @return the sample dimension.
         * @throws IllegalSampleDimensionException if there is overlapping {@linkplain Category#getSampleRange()
         *         ranges of sample values} or other problems that prevent the construction of sample dimensions.
         */
        public SampleDimension build() {
            GenericName name = dimensionName;
defName:    if (name == null) {
                for (int i = 0; i < count; i++) {
                    if (categories[i].isQuantitative()) {
                        name = createLocalName(categories[i].name);
                        break defName;
                    }
                }
                name = createLocalName(Vocabulary.formatInternational(Vocabulary.Keys.Untitled));
            }

            // Putting background defaulting in a dedicated method make it easier to disable/evolve.
            verifyBackground();

            return new SampleDimension(name, toNaN.background, UnmodifiableArrayList.wrap(categories, 0, count));
        }

        /**
         * Try to define a background value if none has been defined by user.
         * Note that this is a best-effort. It analyzes available categories types and names. If it finds a very common
         * case in category naming/typing, it will promote the related category minimum value as background. Otherwise,
         * the background value is left unset.
         */
        private void verifyBackground() {
            if (toNaN.background != null) return;
            final Logger logger = Logging.getLogger("org.apache.sis.coverage");
            try {
                Arrays.stream(categories, 0, count)
                        .map(this::score)
                        .filter(it -> it != null && it.score > 0)
                        .sorted(Comparator.comparing(BackgroundCandidate::getScore).reversed())
                        .findFirst()
                        .ifPresent(promoted -> {
                            logger.log(Level.FINE,
                                    "No background value set by user. Defaulting to {}." +
                                            "Use `setBackground()` to short this automatic choice.",
                                    promoted.category);
                            toNaN.background = promoted.category.getSampleRange().getMinValue();
                        });
            } catch (Exception e) {
                logger.log(Level.FINEST, "Defaulting background value failed", e);
            }
        }

        /**
         * Compute a score for a category. The score represent the chances for the category to be a good replacement for
         * an explicitly set background. The higher the score, the better. Note that any result with a 0 score should be
         * rejected.
         *
         * @param candidate A category to evaluate as a candidate for background value. Do not accept null values.
         * @return The input category with associated score. Can be null if input category is not a good candidate.
         */
        private BackgroundCandidate score(Category candidate) {
            if (candidate.isQuantitative() || candidate.getSampleRange().getMinValue() == null) return null;

            final String enName = candidate.getName()
                    .toString(Locale.ENGLISH)
                    .toLowerCase(Locale.ENGLISH);
            if (enName.equals("background")) return new BackgroundCandidate(1f, candidate);
            else if (enName.equals("fill-value") || enName.equals("fill_value") || enName.equals("fill")) return new BackgroundCandidate(0.9f, candidate);
            else if (enName.equals("no-data") || enName.equals("no_data") || enName.equals("no data")) return new BackgroundCandidate(0.8f, candidate);
            else if (enName.equals("missing-value") || enName.equals("missing_value") || enName.equals("missing value")) return new BackgroundCandidate(0.7f, candidate);
            else if (enName.equals("missing-data") || enName.equals("missing_data") || enName.equals("missing data")) return new BackgroundCandidate(0.6f, candidate);
            else return new BackgroundCandidate(0f, candidate);
        }

        /**
         * Model a category associated with a score that represent its probability to be the best match for a background
         * value.
         */
        private static final class BackgroundCandidate {
            final float score;
            final Category category;

            public BackgroundCandidate(float score, Category category) {
                this.score = score;
                this.category = category;
            }

            public float getScore() { return score; }
        }

        /**
         * Reset this builder to the same state than after construction.
         * The sample dimension name, background values and all categories are discarded.
         * This method can be invoked when the same builder is reused for creating more than one sample dimension.
         */
        public void clear() {
            dimensionName = null;
            count = 0;
            Arrays.fill(categories, null);
            toNaN.clear();
        }
    }
}
