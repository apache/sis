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
 * <table>
 * <tr><th colspan="2">Primitives and classes</th></tr>
 * <tr><td>{@link CharSequences}</td>
 *     <td>Methods working on {@link CharSequence} or {@link String} instances.</td></tr>
 *
 * <tr><th colspan="2" bgcolor="lightblue">Structures (trees, collections, arrays, parameters)</th></tr>
 * <tr><td>{@link Arrays}</td>
 *     <td>Insert or remove elements in the middle of arrays.</td></tr>
 *
 * <tr><th colspan="2">Loggings and exceptions</th></tr>
 * <tr><td>{@link ArgumentChecks}</td>
 *     <td>Perform argument checks and throw {@link IllegalArgumentException} if needed.</td></tr>
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
