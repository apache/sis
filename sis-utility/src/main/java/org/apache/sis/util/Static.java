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
 * Parent of classes that contain only static utility methods. This parent is for documentation
 * purpose only. The list below summarizes some of the utility classes:
 *
 * <table class="sis">
 * <tr><th colspan="2">Basic classes of the Java language</th></tr>
 * <tr><td>{@link Characters}</td>
 *     <td>Find subscript and superscript digit characters.</td></tr>
 * <tr><td>{@link CharSequences}</td>
 *     <td>Methods working on {@link CharSequence} or {@link String} instances.</td></tr>
 * <tr><td>{@link StringBuilders}</td>
 *     <td>Methods modifying {@link StringBuilder} content in-place.</td></tr>
 * <tr><td>{@link Numbers}</td>
 *     <td>Conversions between different kind of {@link Number}.</td></tr>
 * <tr><td>{@link Classes}</td>
 *     <td>Methods working on {@link Class} instances.</td></tr>
 *
 * <tr><th colspan="2" class="hsep">Mathematics and units of measurement</th></tr>
 * <tr><td>{@link org.apache.sis.math.MathFunctions}</td>
 *     <td>Additions to the {@link java.lang.Math} methods.</td></tr>
 * <tr><td>{@link org.apache.sis.measure.Units}</td>
 *     <td>Get a {@linkplain javax.measure.unit.Unit unit} from a symbol or EPSG code,
 *         and test if a unit is angular, linear or temporal.</td></tr>
 *
 * <tr><th colspan="2" class="hsep">Structures (trees, collections, arrays, parameters)</th></tr>
 * <tr><td>{@link org.apache.sis.util.collection.Collections}</td>
 *     <td>Additions to the JDK {@link java.util.Collections} methods.</td></tr>
 * <tr><td>{@link ArraysExt}</td>
 *     <td>Insert or remove elements in the middle of arrays.</td></tr>
 *
 * <tr><th colspan="2" class="hsep">Input / Output (including CRS, XML, images)</th></tr>
 * <tr><td>{@link org.apache.sis.io.IO}</td>
 *     <td>Methods working on {@link Appendable} instances.</td></tr>
 * <tr><td>{@link org.apache.sis.xml.XML}</td>
 *     <td>Marshal or unmarshal ISO 19115 objects.</td></tr>
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
 *     <td>Creates {@link ObjectConverter} instances, or collection views using object converters.</td></tr>
 * </table>
 *
 * @author Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
public class Static {
    /**
     * Do not allow instantiation. This construction is defined only in order to allow
     * subclassing. Subclasses shall declare their own private constructor in order to
     * prevent instantiation.
     */
    protected Static() {
    }
}
