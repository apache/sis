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
package org.apache.sis.util;


/**
 * Parent of SIS classes that contain only static utility methods, for documentation purpose.
 * The list below summarizes some of the utility classes:
 *
 * <table class="sis">
 * <caption>Static utility classes (non exhaustive list)</caption>
 * <tr><th colspan="2">Classes of the Java language</th></tr>
 * <tr><td>{@link Characters}</td>
 *     <td>Find subscript and superscript digit characters.</td></tr>
 * <tr><td>{@link CharSequences}</td>
 *     <td>Methods working on {@link CharSequence} or {@link String} instances.</td></tr>
 * <tr><td>{@link StringBuilders}</td>
 *     <td>Methods modifying {@link StringBuilder} content in-place.</td></tr>
 * <tr><td>{@link Numbers}</td>
 *     <td>Methods working with {@link Number} instances (include conversions between different types).</td></tr>
 * <tr><td>{@link Classes}</td>
 *     <td>Methods working with {@link Class} instances.</td></tr>
 * <tr><td>{@link org.apache.sis.util.collection.Containers}</td>
 *     <td>Additions to the JDK {@link java.util.Collections} methods, also related to the
 *         {@link org.apache.sis.util.collection.CheckedContainer} interface.</td></tr>
 * <tr><td>{@link ArraysExt}</td>
 *     <td>Additions to the JDK {@link java.util.Arrays} methods
 *         (include insert or remove of elements in the middle of arrays).</td></tr>
 *
 * <tr><th colspan="2" class="hsep">Mathematics and units of measurement</th></tr>
 * <tr><td>{@link org.apache.sis.math.MathFunctions}</td>
 *     <td>Additions to the {@link java.lang.Math} methods.</td></tr>
 * <tr><td>{@link org.apache.sis.math.DecimalFunctions}</td>
 *     <td>Mathematical methods related to base 10 representation of numbers.</td></tr>
 * <tr><td>{@link org.apache.sis.referencing.operation.matrix.Matrices}</td>
 *     <td>Create and compare {@link org.opengis.referencing.operation.Matrix} objects</td></tr>
 * <tr><td>{@link org.apache.sis.measure.Units}</td>
 *     <td>Get a {@linkplain javax.measure.unit.Unit unit} from a symbol or EPSG code,
 *         and test if a unit is angular, linear or temporal.</td></tr>
 *
 * <tr><th colspan="2" class="hsep">OGC/ISO objects (metadata, referencing, geometries)</th></tr>
 * <tr><td>{@link org.apache.sis.util.iso.Types}</td>
 *     <td>UML identifier and description for GeoAPI types.</td></tr>
 * <tr><td>{@link org.apache.sis.util.iso.Names}</td>
 *     <td>Simple creation and operations on {@link org.opengis.util.GenericName} objects.</td></tr>
 * <tr><td>{@link org.apache.sis.metadata.iso.citation.Citations}</td>
 *     <td>Pre-defined {@link org.opengis.metadata.citation.Citation}
 *         and methods for comparing against titles or identifiers.</td></tr>
 * <tr><td>{@link org.apache.sis.metadata.iso.extent.Extents}</td>
 *     <td>Extract information from {@link org.opengis.metadata.extent.Extent} objects.</td></tr>
 * <tr><td>{@link org.apache.sis.geometry.Envelopes}</td>
 *     <td>Parse, format and transform {@link org.opengis.geometry.Envelope} objects.</td></tr>
 * <tr><td>{@link org.apache.sis.referencing.IdentifiedObjects}</td>
 *     <td>Handle names, identifiers or properties of
 *         {@link org.opengis.referencing.IdentifiedObject} instances.</td></tr>
 * <tr><td>{@link org.apache.sis.referencing.cs.CoordinateSystems}</td>
 *     <td>Parses axis names and creates transforms between {@link org.opengis.referencing.cs.CoordinateSystem}
 *         instances.</td></tr>
 * <tr><td>{@link org.apache.sis.parameter.Parameters}</td>
 *     <td>Creates, searches or modifies {@link org.opengis.parameter.ParameterValue} instances
 *         in a group of parameters.</td></tr>
 *
 * <tr><th colspan="2" class="hsep">Input / Output (including CRS, XML, images)</th></tr>
 * <tr><td>{@link org.apache.sis.io.IO}</td>
 *     <td>Methods working on {@link Appendable} instances.</td></tr>
 * <tr><td>{@link org.apache.sis.storage.DataStores}</td>
 *     <td>Read or write geospatial data in various backends.</td></tr>
 * <tr><td>{@link org.apache.sis.xml.XML}</td>
 *     <td>Marshal or unmarshal ISO 19115 objects.</td></tr>
 * <tr><td>{@link org.apache.sis.xml.Namespaces}</td>
 *     <td>{@code String} constants for commonly used namespaces.</td></tr>
 *
 * <tr><th colspan="2" class="hsep">Loggings and exceptions</th></tr>
 * <tr><td>{@link ArgumentChecks}</td>
 *     <td>Perform argument checks and throw {@link IllegalArgumentException} if needed.</td></tr>
 * <tr><td>{@link org.apache.sis.util.Exceptions}</td>
 *     <td>Format a stack trace summary or change the exception message.</td></tr>
 * <tr><td>{@link org.apache.sis.util.logging.Logging}</td>
 *     <td>Get a JDK {@linkplain java.util.logging.Logger logger}, which may be a wrapper around
 *         the <cite>Apache Commons Logging</cite> or <cite>Log4J</cite> framework.</td></tr>
 *
 * <tr><th colspan="2" class="hsep">Factories</th></tr>
 * <tr><td>{@link ObjectConverters}</td>
 *     <td>Create {@link ObjectConverter} instances, or collection views using object converters.</td></tr>
 * </table>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class Static {
    /**
     * For subclasses only.
     * Subclasses shall declare a private constructor for preventing instantiation.
     */
    protected Static() {
    }
}
