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
package org.apache.sis.style.se1;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.citation.OnlineResource;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;

// Branch-dependent imports
import org.apache.sis.filter.Filter;


/**
 * Rendering instructions grouped by feature-property conditions and map scales.
 * A rule consists of two important parts: a {@linkplain Filter filter} and a list of symbols.
 * When drawing a given feature, the rendering engine examines each rule in the style,
 * first checking its filter. If the feature is accepted by the filter,
 * then all {@link Symbolizer} for that rule are applied to the given feature.
 *
 * <!-- Following list of authors contains credits to OGC GeoAPI 2 contributors. -->
 * @author  Johann Sorel (Geomatys)
 * @author  Chris Dillard (SYS Technologies)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @param <R>  the type of data to style, such as {@code Feature} or {@code Coverage}.
 *
 * @since 1.5
 */
@XmlType(name = "RuleType", propOrder = {
    "name",
    "description",
    "legend",
//  "filter",           // XML encoding not yet available.
    "elseFilter",
    "minScale",
    "maxScale",
    "symbolizers"
})
@XmlRootElement(name = "Rule")
public class Rule<R> extends StyleElement<R> {
    /**
     * Name for this rule, or {@code null} if none.
     *
     * @see #getName()
     * @see #setName(String)
     */
    @XmlElement(name = "Name")
    protected String name;

    /**
     * Information for user interfaces, or {@code null} if none.
     *
     * @see #getDescription()
     * @see #setDescription(Description)
     */
    @XmlElement(name = "Description")
    protected Description<R> description;

    /**
     * Small graphic to draw in a legend window, or {@code null} if none.
     *
     * @see #getLegend()
     * @see #setLegend(LegendGraphic)
     */
    @XmlElement(name = "LegendGraphic")
    protected LegendGraphic<R> legend;

    /**
     * Filter that will limit the features, or {@code null} if none.
     *
     * @see #getFilter()
     * @see #setFilter(Filter)
     */
//  @XmlElement(name = "Filter", namespace = "http://www.opengis.net/ogc")
    protected Filter<R> filter;

    /**
     * Whether this {@code Rule} will be applied only if no other rules in the containing style apply.
     *
     * @see #isElseFilter()
     * @see #setElseFilter(boolean)
     */
    protected boolean isElseFilter;

    /**
     * Minimum value (inclusive) in the denominator of map scale at which this rule will apply.
     *
     * @see #getMinScaleDenominator()
     * @see #setMaxScaleDenominator(double)
     */
    protected double minScale;

    /**
     * Maximum value (exclusive) in the denominator of map scale at which this rule will apply.
     *
     * @see #getMaxScaleDenominator()
     * @see #setMaxScaleDenominator(double)
     */
    protected double maxScale;

    /**
     * Description of how a feature is to appear on a map.
     *
     * @see #symbolizers()
     */
    @XmlElementRef(name = "Symbolizer")
    private List<Symbolizer<R>> symbolizers;

    /**
     * If the style comes from an external XML file, the original source. Otherwise {@code null}.
     *
     * @see #getOnlineResource()
     * @see #setOnlineSource(OnlineResource)
     */
    protected OnlineResource onlineSource;

    /**
     * For JAXB unmarshalling only.
     */
    private Rule() {
        // Thread-local factory will be used.
    }

    /**
     * Creates an initially empty rule.
     *
     * @param  factory  the factory to use for creating expressions and child elements.
     */
    public Rule(final StyleFactory<R> factory) {
        super(factory);
        maxScale = Double.POSITIVE_INFINITY;
        symbolizers = new ArrayList<>();
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public Rule(final Rule<R> source) {
        super(source);
        name         = source.name;
        description  = source.description;
        legend       = source.legend;
        filter       = source.filter;
        minScale     = source.minScale;
        maxScale     = source.maxScale;
        onlineSource = source.onlineSource;
        symbolizers  = new ArrayList<>(source.symbolizers);
    }

    /**
     * Returns the name for this rule.
     * This can be any string that uniquely identifies this rule within a given canvas.
     * It is not meant to be human-friendly. For a human-friendly label,
     * see the {@linkplain Description#getTitle() title} instead.
     *
     * @return a name for this rule.
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Sets a name for this rule.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new name for this rule, or {@code null} if none.
     */
    public void setName(final String value) {
        name = value;
    }

    /**
     * Returns the description of this rule.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this rule, and conversely.
     *
     * @return information for user interfaces.
     */
    public Optional<Description<R>> getDescription() {
        return Optional.ofNullable(description);
    }

    /**
     * Sets a description of this rule.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new information for user interfaces, or {@code null} if none.
     */
    public void setDescription(final Description<R> value) {
        description = value;
    }

    /**
     * Returns a small graphic that could be used by the rendering engine to draw a legend window.
     * User interfaces may present the user with a legend that indicates how features of a given type are being portrayed.
     * Through its {@code LegendGraphic} property, a {@code Rule} can provide a custom picture to be used in such a legend window.
     *
     * <p>The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this rule, and conversely.</p>
     *
     * @return small graphic to draw in a legend window.
     */
    public Optional<LegendGraphic<R>> getLegend() {
        return Optional.ofNullable(legend);
    }

    /**
     * Sets a small graphic that could be used by the rendering engine to draw a legend window.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new legend graphic, or {@code null} if none.
     */
    public void setLegend(final LegendGraphic<R> value) {
        legend = value;
    }

    /**
     * Returns the filter that will limit the features for which this rule will apply.
     * This value should be used only if {@link #isElseFilter()} returns {@code false},
     * in which case this rule applies to all features.
     *
     * @return the filter that will limit the features.
     */
    public Filter<R> getFilter() {
        if (isElseFilter) {
            return Filter.exclude();
        }
        final var value = filter;
        return (value != null) ? value : Filter.include();
    }

    /**
     * Sets the filter that will limit the features for which this rule will apply.
     * If this method is never invoked, then the default value is {@link Filter#include()}.
     * Invoking this method forces {@link #isElseFilter()} to {@code false}.
     *
     * @param  value  new filter that will limit the features, or {@code null} if none.
     */
    public void setFilter(final Filter<R> value) {
        isElseFilter = false;
        filter = value;
    }

    /**
     * Returns true if this {@code Rule} will be applied only if no other rules in the containing style apply.
     * If this is true, then the {@linkplain #getFilter() filter} should be ignored.
     *
     * <p>The "Else Filter" is implicitly a filter with a condition that depends on the enclosing style.
     * Consequently, it cannot be expressed as a standalone {@code Filter} expression in this rule.</p>
     *
     * @return true if the filter is an else filter.
     */
    public boolean isElseFilter() {
        return isElseFilter;
    }

    /**
     * Sets or unset this filter to an else filter.
     *
     * @param  value  whether the filter is the "else" filter.
     */
    public void setElseFilter(final boolean value) {
        isElseFilter = value;
    }

    /**
     * Invoked by JAXB at marshalling time for expressing the boolean {@code isElseFilter} value as an XML element.
     */
    @XmlElement(name = "ElseFilter")
    private ElseFilter getElseFilter() {
        return isElseFilter ? ElseFilter.INSTANCE : null;
    }

    /**
     * Invoked at JAXB unmarshalling time when an {@code <ElseFilter/>} element is found.
     */
    private void setElseFilter(final ElseFilter value) {
        isElseFilter = (value != null);
    }

    /**
     * Returns the minimum value (inclusive) in the denominator of map scale at which this rule will apply.
     * If, for example, this value was 10000, then this rule would only apply at scales of 1:<var>X</var>
     * where <var>X</var> is greater than 10000. A value of zero indicates that there is no minimum.
     *
     * <h4>Relationship with real world lengths</h4>
     * The values used are scale denominators relative to a “standardized rendering pixel size”.
     * That size is defined as a square with sides of 0.28 millimeters. If the real pixel size
     * is different or if the CRS uses angular units instead of linear, then the renderer shall
     * take those information in account as described in OGC 05-077r4 §10.2.
     *
     * @return minimum scale value, inclusive.
     */
    public double getMinScaleDenominator() {
        return minScale;
    }

    /**
     * Invoked by JAXB for marshalling the minimum scale denominator.
     * If both the minimum and maximum are the default values, then this property is omitted.
     * If a maximum value exists, then the zero minimum is explicitly written for clarity.
     */
    @XmlElement(name = "MinScaleDenominator")
    private Double getMinScale() {
        final var value = minScale;
        return (value > 0 || maxScale != Double.POSITIVE_INFINITY) ? value : null;
    }

    /**
     * Sets the minimum value (inclusive) in the denominator of map scale at which this rule will apply.
     * If the given value is greater than the maximum scale, then that maximum is discarded.
     * If this method is never invoked, then the default value is 0.
     *
     * @param  value  new minimum scale value (inclusive).
     */
    public void setMinScaleDenominator(final double value) {
        ArgumentChecks.ensurePositive("MinScaleDenominator", value);
        minScale = value;
        if (value > maxScale) {
            maxScale = Double.POSITIVE_INFINITY;
        }
    }

    /**
     * Invoked by JAXB for unmarshalling the minimum scale denominator.
     * The argument validity check assumes that this method is invoked only once.
     * If this assumption is violated, the check allows range restriction but not expansion.
     * If the given value is invalid, a warning is reported but the unmarshalling continue.
     */
    private void setMinScale(final Double value) {
        if (isValidScale("MinScaleDenominator", value)) {
            minScale = value;
        }
    }

    /**
     * Returns the maximum value (exclusive) in the denominator of map scale at which this rule will apply.
     * If, for example, this value was 10000, then this rule would only apply at scales of 1:<var>X</var>
     * where <var>X</var> is less than 10000.
     * An {@linkplain Double#POSITIVE_INFINITY infinite} value indicates that there is no maximum.
     *
     * <h4>Relationship with real world lengths</h4>
     * The same discussion than {@link #getMinScaleDenominator()} applies also to the maximum scale value.
     *
     * @return maximum scale value, exclusive.
     */
    public double getMaxScaleDenominator() {
        return maxScale;
    }

    /**
     * Invoked by JAXB for marshalling the maximum scale denominator.
     * If the value is positive infinity, then the property is omitted.
     */
    @XmlElement(name = "MaxScaleDenominator")
    private Double getMaxScale() {
        final var value = maxScale;
        return (value != Double.POSITIVE_INFINITY) ? value : null;
    }

    /**
     * Sets the maximum value (exclusive) in the denominator of map scale at which this rule will apply.
     * If the given value is less than the minimum scale, then that minimum is discarded.
     * If this method is never invoked, then the default value is {@link Double#POSITIVE_INFINITY}.
     *
     * @param  value  new maximum scale value (exclusive).
     */
    public void setMaxScaleDenominator(final double value) {
        ArgumentChecks.ensureStrictlyPositive("MaxScaleDenominator", value);
        maxScale = value;
        if (value < minScale) {
            minScale = 0;
        }
    }

    /**
     * Invoked by JAXB for unmarshalling the maximum scale denominator.
     * The argument validity check assumes that this method is invoked only once.
     * If this assumption is violated, the check allows range restriction but not expansion.
     * If the given value is invalid, a warning is reported but the unmarshalling continue.
     */
    private void setMaxScale(final Double value) {
        if (isValidScale("MaxScaleDenominator", value)) {
            maxScale = value;
        }
    }

    /**
     * Indicates whether an unmarshalled minimum or maximum scale denominator is inside the expected range of values.
     * If the given value is invalid, a warning is emitted to the JAXB unmarshaller and the caller will keep the default value.
     */
    private boolean isValidScale(final String name, final Double value) {
        boolean isValidScale = (value != null);
        if (isValidScale) {
            isValidScale = (value >= minScale && value <= maxScale);
            if (!isValidScale) {
                Context.warningOccured(Context.current(), Rule.class, "set".concat(name), Errors.class,
                                       Errors.Keys.ValueOutOfRange_4, name, minScale, maxScale, value);
            }
        }
        return isValidScale;
    }

    /**
     * Returns the description of how a feature is to appear on a map.
     * Each symbolizer describes how the shape should appear,
     * together with graphical properties such as color and opacity.
     * The predefined type of symbolizers are
     * {@linkplain LineSymbolizer line},
     * {@linkplain PolygonSymbolizer polygon},
     * {@linkplain PointSymbolizer point},
     * {@linkplain TextSymbolizer text}, and
     * {@linkplain RasterSymbolizer raster} symbolizers.
     *
     * <p>The returned collection is <em>live</em>:
     * changes in that collection are reflected into this object, and conversely.</p>
     *
     * @return the list of symbolizers, as a live collection.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<Symbolizer<R>> symbolizers() {
        return symbolizers;
    }

    /**
     * If the style comes from an external XML file, the original source.
     * This property may be non-null if a XML document specified this rule
     * by a link to another XML document.
     *
     * @return the original source of this rule.
     */
    public Optional<OnlineResource> getOnlineSource() {
        return Optional.ofNullable(onlineSource);
    }

    /**
     * If the style comes from an external XML file, the original source.
     * If this method is never invoked, then the default value is absence.
     *
     * <h4>Effect on XML marshalling</h4>
     * Setting this property to a non-null value has the following effect:
     * When this rule is written in a XML document, then instead of writing
     * the XML elements describing this rule,
     * the specified link will be written instead.
     *
     * @todo Above-describing marshalling is not yet implemented.
     *
     * @param  value  new source of this rule, or {@code null} if none.
     */
    public void setOnlineSource(final OnlineResource value) {
        onlineSource = value;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {name, description, legend, filter, minScale, maxScale, symbolizers, onlineSource};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public Rule<R> clone() {
        final var clone = (Rule<R>) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    private void selfClone() {
        if (description != null) description = description.clone();
        if (legend      != null) legend      = legend.clone();
        symbolizers = new ArrayList<>(symbolizers);
        symbolizers.replaceAll(Symbolizer::clone);
    }
}
