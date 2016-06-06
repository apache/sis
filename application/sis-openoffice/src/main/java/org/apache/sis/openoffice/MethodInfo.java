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
package org.apache.sis.openoffice;


/**
 * Information about a method to be exported as Apache OpenOffice add-in.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class MethodInfo {
    /** The category name. */
    final String category;

    /** The display name. */
    final String display;

    /** A description of the exported method. */
    final String description;

    /** Arguments names (even index) and descriptions (odd index). */
    final String[] arguments;

    /**
     * Constructs method informations.
     *
     * @param category     the category name.
     * @param display      the display name.
     * @param description  a description of the exported method.
     * @param arguments    arguments names (even index) and descriptions (odd index).
     */
    MethodInfo(final String category,
               final String display,
               final String description,
               final String[] arguments)
    {
        this.category    = category;
        this.display     = display;
        this.description = description;
        this.arguments   = arguments;
    }
}
