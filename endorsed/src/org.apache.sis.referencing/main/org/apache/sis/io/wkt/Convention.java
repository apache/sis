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
package org.apache.sis.io.wkt;

import org.opengis.metadata.citation.Citation;
import org.apache.sis.util.Debug;
import org.apache.sis.metadata.iso.citation.Citations;


/**
 * The convention to use for <abbr title="Well Known Text">WKT</abbr> formatting.
 * This enumeration specifies whether to use the <i>Well Known Text</i> format defined by <abbr>ISO</abbr> 19162
 * (also known as “WKT 2”), or whether to use the format previously defined in OGC 01-009 (referenced as “WKT 1”).
 *
 * <h2><abbr title="Well Known Text">WKT</abbr> 1 variants</h2>
 * The <abbr>WKT</abbr> 2 format should be parsed and formatted consistently by all software products.
 * But the <abbr>WKT</abbr> 1 format has been interpreted differently by various implementers.
 * Apache SIS can adapt itself to different WKT variants, sometimes automatically. But some aspects cannot be guessed.
 * One noticeable source of confusion is the unit of measurement of {@code PRIMEM[…]} and {@code PARAMETER[…]} elements:
 *
 * <ul>
 *   <li>The unit of the Prime Meridian shall be the angular unit of the enclosing Geographic CRS
 *       according the OGC 01-009 (<cite>Coordinate transformation services</cite>) specification.</li>
 *   <li>An older specification — <cite>Simple Features</cite> — was unclear on this matter and has been
 *       interpreted by many software products as fixing the unit to decimal degrees.</li>
 *   <li>Some software products support only (<var>longitude</var>, <var>latitude</var>) axis order
 *       and ignore completely all {@code AXIS[…]} elements in the WKT.</li>
 * </ul>
 *
 * Despite the first interpretation being specified by both OGC 01-009 and ISO 19162 standards, the second
 * interpretation appears to be in wide use for WKT 1. Apache SIS uses the standard interpretation by default,
 * but the {@link #WKT1_COMMON_UNITS} enumeration allows parsing and formatting using the older interpretation.
 * The {@link #WKT1_IGNORE_AXES} enumeration mimics the most minimalist <abbr>WKT</abbr> 1 parsers,
 * but should be avoided when not imposed by compatibility reasons.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see WKTFormat#getConvention()
 * @see WKTFormat#setConvention(Convention)
 *
 * @since 0.4
 */
public enum Convention {
    /*
     * NOTE: Enumeration order matter. It should be roughly from most recent versions to oldest versions.
     */

    /**
     * Latest version of the ISO 19162 format (also known as “WKT 2”) supported by Apache SIS.
     * In the current version of Apache <abbr>SIS</abbr>, this is synonymous of {@link #WKT2_2019}.
     *
     * <p>This is the default convention used by {@link FormattableObject#toWKT()}
     * and for new {@link WKTFormat} instances.</p>
     */
    WKT2(true),

    /**
     * The ISO 19162 format with omission of some optional elements. This convention is identical
     * to the {@link #WKT2} convention except for the following aspects:
     *
     * <ul>
     *   <li>By {@linkplain KeywordStyle#DEFAULT default}, this convention uses the keywords that are the closest matches
     *       to the Java interface names. For example, {@code "GeodeticCRS"} is preferred to {@code "GeodCRS"}.</li>
     *   <li>The {@code PrimeMeridian} element is omitted if the meridian is Greenwich.</li>
     *   <li>The {@code Axis} element omits always the {@code Order} sub-element.</li>
     *   <li>The {@code Axis} element omits the {@code AxisMinValue}, {@code AxisMinValue} and {@code RangeMeaning}
     *       sub-elements if their values are the standard values for latitude or longitude axes.</li>
     *   <li>The {@code Unit} elements are less verbose:<ul>
     *     <li>{@code Ellipsoid} and {@code VerticalExtent} elements omit the {@code LengthUnit} sub-element
     *         if that unit is {@link org.apache.sis.measure.Units#METRE}.</li>
     *     <li>{@code Parameter} elements omit the {@code LengthUnit} sub-element
     *         if that unit is the same as the unit of the {@code ProjectedCRS} axes.</li>
     *     <li>{@code Parameter} and {@code PrimeMeridian} elements omit the {@code AngleUnit} sub-element
     *         if that unit is the same as the unit of the {@code GeodeticCRS} axes.</li>
     *     <li>Axes unit is declared only once after the axes instead of repeated for each axis
     *         if the unit is the same for all axes.</li>
     *     <li>{@code AngleUnit}, {@code LengthUnit}, {@code ScaleUnit}, {@code ParametricUnit}
     *         and {@code TimeUnit} are formatted as plain {@code Unit} elements.</li>
     *     </ul></li>
     *   <li>The {@code Id} element is formatted only for the root element
     *       (omit parameters and operation methods {@code Id}).</li>
     * </ul>
     *
     * Those modifications are allowed by the ISO 19162 standard, so the WKT is still valid.
     *
     * <p>This is the default convention used by {@link FormattableObject#toString()}.</p>
     */
    WKT2_SIMPLIFIED(false),

    /**
     * The ISO 19162:2019 format, also known as “WKT 2”.
     * This version replaces ISO 19162:2015.
     *
     * <p>Unless otherwise specified by {@link WKTFormat#setNameAuthority(Citation)}, projections
     * and parameters formatted with this convention will use the {@linkplain Citations#EPSG EPSG}
     * names when available.</p>
     *
     * @since 1.5
     */
    WKT2_2019(true),

    /**
     * The ISO 19162:2015 format, also known as “WKT 2”.
     * This version has been replaced by ISO 19162:2019.
     * This enumeration value can be used when compatibility with this older standard is required.
     *
     * <p>This was the default convention used by {@link FormattableObject#toWKT()}
     * in Apache <abbr>SIS</abbr> versions prior to version 1.5.</p>
     *
     * @since 1.5
     */
    WKT2_2015(true),

    /**
     * The OGC 01-009 format, also known as “WKT 1”.
     * A definition for this format is shown in Extended Backus Naur Form (EBNF) in OGC 01-009.
     *
     * <p>Unless otherwise specified by {@link WKTFormat#setNameAuthority(Citation)}, projections
     * and parameters formatted with this convention will use the {@linkplain Citations#OGC OGC}
     * names when available.</p>
     *
     * <h4>Differences compared to WKT 2</h4>
     * WKT 1 and WKT 2 differ in their keywords and syntax, but also in more subtle ways regarding axis names,
     * parameter and code list values. For example, for geocentric CRS, WKT 1 uses a legacy set of Cartesian axes
     * which were defined in OGC 01-009. Those axes use the <var>Other</var>, <var>Easting</var> and <var>Northing</var>
     * {@linkplain org.opengis.referencing.cs.AxisDirection axis directions} instead of the geocentric ones.
     * For more uniform handling of CRS objects in client code, SIS parser replaces some WKT 1 conventions by
     * the ISO ones when possible.
     *
     * <div class="horizontal-flow">
     * <div>
     * <table class="sis">
     *   <caption>Geocentric axis directions</caption>
     *   <tr><th>ISO 19111</th>    <th>OGC 01-009</th> <th>Description</th></tr>
     *   <tr><td>Geocentric X</td> <td>Other</td>      <td>Toward prime meridian</td></tr>
     *   <tr><td>Geocentric Y</td> <td>Easting</td>    <td>Toward 90°E longitude</td></tr>
     *   <tr><td>Geocentric Z</td> <td>Northing</td>   <td>Toward north pole</td></tr>
     * </table>
     * </div><div>
     * <table class="sis">
     *   <caption>Coordinate system axis names</caption>
     *   <tr><th>CRS type</th>   <th>WKT1 names</th>                               <th>ISO abbreviations</th></tr>
     *   <tr><td>Geographic</td> <td>Lon, Lat</td>                                 <td>λ, φ</td></tr>
     *   <tr><td>Vertical</td>   <td><var>H</var></td>                             <td><var>H</var> or <var>h</var></td></tr>
     *   <tr><td>Projected</td>  <td><var>X</var>, <var>Y</var></td>               <td><var>E</var>, <var>N</var></td></tr>
     *   <tr><td>Geocentric</td> <td><var>X</var>, <var>Y</var>, <var>Z</var></td> <td><var>X</var>, <var>Y</var>, <var>Z</var></td></tr>
     * </table>
     * </div></div>
     */
    WKT1(true),

    /**
     * The <cite>Simple Feature</cite> format, also known as “WKT 1”.
     * <cite>OGC Simple Feature</cite> is anterior to OGC 01-009 and defines the same format,
     * but was unclear about the unit of measurement for prime meridians and projection parameters.
     * Consequently, many implementations interpreted those angular units as fixed to degrees instead
     * than being context-dependent.
     *
     * <p>This convention is identical to {@link #WKT1} except for the following aspects:</p>
     * <ul>
     *   <li>The angular units of {@code PRIMEM} and {@code PARAMETER} elements are always degrees,
     *       no matter the units of the enclosing {@code GEOGCS} element.</li>
     *   <li>Unit names use American spelling instead of the international ones
     *       (e.g. <q>meter</q> instead of <q>metre</q>).</li>
     * </ul>
     */
    WKT1_COMMON_UNITS(true),

    /**
     * The <cite>Simple Feature</cite> format without parsing of axis elements.
     * This convention is identical to {@link #WKT1_COMMON_UNITS} except that all {@code AXIS[…]} elements are ignored.
     * Since the WKT 1 specification said that the default axis order shall be (<var>x</var>,<var>y</var>) or
     * (<var>longitude</var>, <var>latitude</var>), ignoring {@code AXIS[…]} elements is equivalent to forcing
     * the coordinate systems to that default order.
     *
     * <p>Note that {@code AXIS[…]} elements still need to be well formed even when parsing a text with this convention.
     * Malformed axis elements will continue to cause a {@link java.text.ParseException} despite their content being ignored.</p>
     *
     * <p>This convention may be useful for compatibility with some other software products that do not handle axis order correctly.
     * But except when imposed by such compatibility reasons, this convention should be avoided as much as possible.</p>
     *
     * @since 0.6
     */
    WKT1_IGNORE_AXES(true),

    /**
     * A special convention for formatting objects as stored internally by Apache SIS.
     * The result is similar to the one produced using the {@link #WKT2_SIMPLIFIED} convention,
     * with the following differences:
     *
     * <ul>
     *   <li>All quoted texts (not only the remarks) preserve non-ASCII characters.</li>
     *   <li>Map projections are shown as SIS stores them internally, i.e. with the separation between
     *       linear and non-linear steps, rather than as a single operation.</li>
     *   <li>{@code Parameter} elements omit the unit of measurement if that unit is equal to the default unit
     *       (as declared in the parameter descriptor).</li>
     *   <li>{@code CompoundCRS} elements show nested compound CRS if any (the structure is not flattened).</li>
     *   <li>{@code Id} elements are formatted for child elements in addition to the root one.</li>
     *   <li>{@code Id} element omits the {@code URI} sub-element if the latter is derived by Apache SIS
     *       from the {@code Id} properties.</li>
     *   <li>{@code Remarks} element is formatted for all
     *       {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject identified objects},
     *       not only CRS or coordinate operations.</li>
     *   <li>Additional attributes not defined by ISO 19162 may be formatted.</li>
     * </ul>
     *
     * This convention is used only for debugging purpose.
     */
    @Debug
    INTERNAL(false);

    /**
     * The default conventions.
     */
    static final Convention DEFAULT = WKT2;

    /**
     * {@code true} for using short upper-case keywords by {@linkplain KeywordStyle#DEFAULT default}.
     */
    final boolean toUpperCase;

    /**
     * Creates a new enumeration value.
     */
    private Convention(final boolean toUpperCase) {
        this.toUpperCase = toUpperCase;
    }

    /**
     * Returns whether this convention supports the feature of the given convention.
     * For example, {@code supports(WKT2_2015)} returns {@code true} if this convention represents
     * either <abbr>ISO</abbr> 19162:2015 or a more recent version such as <abbr>ISO</abbr> 19162:2019.
     *
     * @param  base  the base version which is required.
     * @return whether this convention is the given base version or a more recent version.
     *
     * @since 1.5
     */
    public boolean supports(final Convention base) {
        return compareTo(base) <= 0 || this == INTERNAL;
    }

    /**
     * Returns the major version of the Well Known Text represented by this convention.
     * In current Apache SIS implementation, this method can return only 1 or 2.
     *
     * @return 1 if this convention is one of the WKT 1 variants, or 2 otherwise.
     */
    public int majorVersion() {
        return supports(WKT2_2015) ? 2 : 1;
    }

    /**
     * Returns {@code true} if this convention is one of the simplified variants of WKT.
     * The simplifications are documented in the {@link #WKT2_SIMPLIFIED} javadoc.
     * This method also considers version 1 of WKT as a simplified convention.
     *
     * @return {@code true} it this convention uses a simplified variant of WKT.
     */
    public boolean isSimplified() {
        return this == WKT2_SIMPLIFIED || ordinal() >= WKT1.ordinal();
    }

    /**
     * {@code true} for a frequently-used convention about units instead of the standard one.
     * <ul>
     *   <li>If {@code true}, forces {@code PRIMEM} and {@code PARAMETER} angular units to degrees
     *       instead of inferring the unit from the context. The standard value is {@code false},
     *       which means that the angular units are inferred from the context as required by the
     *       WKT 1 specification.</li>
     *   <li>If {@code true}, uses <abbr>US</abbr> unit names instead of the international names.
     *       For example, Americans said {@code "meter"} instead of {@code "metre"}.</li>
     * </ul>
     */
    final boolean usesCommonUnits() {
        return this == WKT1_COMMON_UNITS || this == WKT1_IGNORE_AXES;
    }

    /**
     * Returns the default authority to look for when fetching identified object names and identifiers.
     * The difference between various authorities are most easily seen in projection and parameter names.
     * The value returned by this method can be overwritten by {@link WKTFormat#setNameAuthority(Citation)}.
     *
     * <h4>Example</h4>
     * The following table shows the names given by various organizations or projects for the same projection:
     *
     * <table class="sis">
     *   <caption>Map projection name examples</caption>
     *   <tr><th>Authority</th> <th>Projection name</th></tr>
     *   <tr><td>EPSG</td>      <td>Mercator (variant A)</td></tr>
     *   <tr><td>OGC</td>       <td>Mercator_1SP</td></tr>
     *   <tr><td>GEOTIFF</td>   <td>CT_Mercator</td></tr>
     * </table>
     *
     * @return the organization, standard or project to look for when fetching Map Projection parameter names.
     *
     * @see WKTFormat#getNameAuthority()
     * @see Citations#EPSG
     * @see Citations#OGC
     */
    final Citation getNameAuthority() {
        return majorVersion() == 1 ? Citations.OGC : Citations.EPSG;
    }
}
